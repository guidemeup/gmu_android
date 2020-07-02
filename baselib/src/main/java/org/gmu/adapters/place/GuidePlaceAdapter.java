package org.gmu.adapters.place;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import org.gmu.base.R;
import org.gmu.config.Constants;

import org.gmu.control.Controller;
import org.gmu.control.GmuEventListener;
import org.gmu.dao.impl.AbstractPlaceElementDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;

import java.util.List;

/**
 * User: ttg
 * Date: 21/01/13
 * Time: 13:29
 * To change this template use File | Settings | File Templates.
 */
public class GuidePlaceAdapter extends PlaceAdapter {
    private static final String TAG = GuidePlaceAdapter.class.getName();

    public GuidePlaceAdapter(Context context, int textViewResourceId,
                             List<PlaceElement> items) {
        super(context, textViewResourceId, items);

    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        PlaceElement p = items.get(position);
        if (view == null) {
            view = inflater.inflate(R.layout.guidelist_row, null);

        } else {
            Holder h = (Holder) view.getTag();


        }


        fillPlaceRow(view, p, context,false);


        return view;
    }

    private static Holder getHolder(View view, final PlaceElement item) {
        if (view.getTag() == null) {
            Holder holder = new Holder();
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.thumbnail = (ImageView) view.findViewById(R.id.thumbnailimg);
            holder.info = (ImageView) view.findViewById(R.id.about);
            holder.shortDescription = (TextView) view.findViewById(R.id.short_description);
            holder.accessButton=(LinearLayout)view.findViewById(R.id.access);
            holder.accessText=(TextView)view.findViewById(R.id.access_text);
            holder.price=(TextView)view.findViewById(R.id.price);
            holder.next=(ImageView)view.findViewById(R.id.imagenext);
            holder.ratingbar= (RatingBar) view.findViewById(R.id.rating_bar);
            holder.accessButton.setVisibility(View.GONE);


            holder.uid = item.getUid();
            view.setTag(holder);
        }
        return (Holder) view.getTag();
    }

    public static void fillPlaceRow(View view, final PlaceElement item, final Context context,boolean showAccessButton) {
        fillPlaceRow(getHolder(view, item), item, context,showAccessButton);
    }

    private static void fillPlaceRow(Holder holder, final PlaceElement item, final Context context,boolean showAccessButton) {


        final String guideUID = item.getUid();

        holder.title.setText(item.getTitle());


        if(!Controller.getInstance().getDao().isPreProductionGuide(item.getUid()))
        {


            if(Controller.getInstance().getConfig().getPurchaseManager()!=null)
            {
                if(Controller.getInstance().getDao().paymentNeeded(item)&&
                        !Utils.isEmpty(item.getAttributes().get("price")))
                {
                        holder.price.setText(item.getAttributes().get("price"));
                }
                else
                {
                    holder.price.setText(" ");
                }
            }else
            {
                holder.price.setText(R.string.free);
            }


        }else
        {   //preproduction indicator
            holder.price.setText("Draft");
        }


        String shortTitle = item.getAttributes().get("shortdescription");
        if (Utils.isEmpty(shortTitle)) {

            holder.shortDescription.setVisibility(View.INVISIBLE);
        } else {
            holder.shortDescription.setText(shortTitle);
            holder.shortDescription.setVisibility(View.VISIBLE);
        }

        if(showAccessButton)
        {   holder.price.setVisibility(View.GONE);
            holder.next.setVisibility(View.INVISIBLE);

            updateAccessGuideText(holder.accessText, item);


        }
        if(!Utils.isEmpty(item.getAttributes().get("rating")))
        {
            holder.ratingbar.setRating(Float.parseFloat(item.getAttributes().get("rating")));
        }



        String mainImg = AbstractPlaceElementDAO.getMainImg(item);
        if (!Utils.isEmpty(mainImg)) {
            Controller.getInstance().getDao().loadImageInView(holder.thumbnail, mainImg, Constants.GUIDE_THUMBNAILDPI);

        } else {
            holder.thumbnail.setImageResource(R.drawable.default_img);
        }
        //info

        if (!Constants.SHOWINFOINGUIDELIST || Utils.isEmpty(item.getAttributes().get("microsite")))
        {
            holder.info.setVisibility(View.GONE);

        } else {
            holder.info.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    //push detail to guide
                    Controller.getInstance().pushDetail(guideUID);
                    //start with list
                    Controller.getInstance().switchView(GmuEventListener.VIEW_MICROSITE);

                }
            });
        }


    }





    public static boolean updateAccessGuideText(TextView accessText,PlaceElement item)
    {
        String initialText=""+accessText.getText();

        accessText.setVisibility(View.VISIBLE);
        if(Controller.getInstance().getDao().paymentNeeded(item))
        {
            if(Utils.isEmpty(item.getAttributes().get("price")))
            {
                accessText.setText("Checking...");
            }
            else
            {
               accessText.setText(item.getAttributes().get("price"));
            }
        }else
        {

            if (!AbstractPlaceElementDAO.isGuideDownloaded(item.getUid()))
            {

                accessText.setText(R.string.download);
            } else
            {
                accessText.setText(R.string.open);

            }
        }

        return Utils.equals(initialText,""+accessText.getText());


    }


    static class Holder {
        TextView title;
        TextView shortDescription;
        ImageView thumbnail;
        ImageView info;
        LinearLayout accessButton;
        TextView accessText;
        TextView price;
        ImageView next;
        RatingBar ratingbar;

        String uid;
    }
}
