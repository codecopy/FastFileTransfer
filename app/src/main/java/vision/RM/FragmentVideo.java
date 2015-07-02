package vision.RM;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import vision.fastfiletransfer.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentVideo.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentVideo#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentVideo extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private ListView lvVio;
    private AdapterVideo mAdapterVideo;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentVideo.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentVideo newInstance(String param1, String param2) {
        FragmentVideo fragment = new FragmentVideo();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentVideo() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);
        lvVio = (ListView)
                rootView.findViewById(R.id.lvVio);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapterVideo = new AdapterVideo(getActivity());
        lvVio.setAdapter(mAdapterVideo);
        new RefreshList().execute();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    //步骤1：通过后台线程AsyncTask来读取数据库，放入更换Cursor
    private class RefreshList extends AsyncTask<Void, Void, SparseArray<?>> {
        SparseArray<Video> videos;

        //步骤1.1：在后台线程中从数据库读取，返回新的游标newCursor
        protected SparseArray<?> doInBackground(Void... params) {
            videos = new SparseArray<Video>();
            Cursor curVideo = getActivity().getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Video.Media._ID,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.DISPLAY_NAME
                    },
                    null,
                    null,
                    MediaStore.Video.Media.DATE_MODIFIED+" DESC");
            if (curVideo.moveToFirst()) {
                Video video;
                int i = 0;
                do {
                    video = new Video();
                    video.id = curVideo.getInt(curVideo.getColumnIndex(MediaStore.Video.Media._ID));
                    video.data = curVideo.getString(curVideo.getColumnIndex(MediaStore.Video.Media.DATA));
                    video.name = curVideo.getString(curVideo
                            .getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                    this.videos.put(i, video);
                } while (curVideo.moveToNext());
            }
            curVideo.close();
            return videos;
        }


        @Override
        protected void onPostExecute(SparseArray<?> sparseArray) {
//            super.onPostExecute(sparseArray);
            mAdapterVideo.setData(sparseArray);
        }

        //步骤1.2：线程最后执行步骤，更换adapter的游标，并奖原游标关闭，释放资源
//        protected void onPostExecute(Cursor newCursor) {

//            adapter.changeCursor(newCursor);//网上看到很多问如何更新ListView的信息，采用CusorApater其实很简单，换cursor就可以
//            cursor.close();
//            cursor = newCursor;
//        }
    }


}
