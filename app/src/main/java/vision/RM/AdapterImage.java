package vision.RM;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import vision.fastfiletransfer.R;

/**
 * Created by Vision on 15/6/30.<br>
 * Email:Vision.lsm.2012@gmail.com
 */
public class AdapterImage extends AdapterList {

    private SparseArray<Image> images;

    public AdapterImage(Context context) {
        super(context);
    }

    @Override
    void initData() {
        images = new SparseArray<Image>();
//        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, "0=0) group by (bucket_display_name", null, null);
        Cursor curImage = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{
                MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA
        }, null, null, MediaStore.Images.Media.DEFAULT_SORT_ORDER);
//        Cursor curThumb = cr.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Images.Thumbnails.IMAGE_ID);
        if (curImage.moveToFirst()) {
//        curThumb.moveToFirst();
            Image image;
            int i = 0;
            do {
                image = new Image();
                image.id = curImage.getInt(curImage.getColumnIndex(MediaStore.Images.Media._ID));
//            images.thumbnails = curThumb.getString(curThumb.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
                image.data = curImage.getString(curImage.getColumnIndex(MediaStore.Images.Media.DATA));
                image.name = curImage.getString(curImage
                        .getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                this.images.put(i, image);
                i++;
            } while (curImage.moveToNext());
//        curThumb.close();
        }
        curImage.close();
    }


    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (null == convertView) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.listitem_image, null);
            holder.layout = (LinearLayout) convertView
                    .findViewById(R.id.list_item_layout);
            holder.image = (ImageView) convertView
                    .findViewById(R.id.image);
            holder.name = (TextView) convertView
                    .findViewById(R.id.name);
            holder.checkBox = (CheckBox)
                    convertView.findViewById(R.id.checkBox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final Image image = this.images.get(position);

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                image.isSelected = isChecked;
            }
        });
        holder.name.setText(image.name);
        holder.checkBox.setChecked(image.isSelected);
        Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(cr, image.id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
        holder.image.setImageBitmap(bm);
        return convertView;
    }

    /**
     * 暂存变量类
     */
    static class ViewHolder {
        LinearLayout layout;
        ImageView image;
        TextView name;
        CheckBox checkBox;
    }


    private class Image {
        public int id;
        public String data;
        public String name;
        public boolean isSelected;
    }
}