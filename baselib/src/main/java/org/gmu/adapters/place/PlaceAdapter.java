package org.gmu.adapters.place;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.gmu.config.Constants;
import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.impl.AbstractPlaceElementDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.translation.CategoryTranslationTable;
import org.gmu.utils.Utils;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 8/11/12
 * Time: 17:30
 * To change this template use File | Settings | File Templates.
 */
public class PlaceAdapter extends ArrayAdapter<PlaceElement> {


    protected Context context;
    protected List<PlaceElement> items;
    protected LayoutInflater inflater = null;
    private static final boolean SHOWPARENTSEPARATOR = false;


    public PlaceAdapter(Context context, int textViewResourceId,
                        List<PlaceElement> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.items = items;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        PlaceElement item = items.get(position);
        if (view == null) {
            view = inflater.inflate(R.layout.placelist_row, null);

        } else {
            PlaceHolder h = (PlaceHolder) view.getTag();


        }


        fillPlaceRow(view, item, context, Controller.getInstance().getConfig().getShowDistance());


        if (SHOWPARENTSEPARATOR && position == 1) {        //show separator
            ((TextView) view.findViewById(R.id.separator)).setVisibility(View.VISIBLE);

        }


        return view;
    }

    private static PlaceHolder getHolder(View view, PlaceElement item) {
        if (view.getTag() == null) {
            PlaceHolder holder = new PlaceHolder();
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.icon = (ImageView) view.findViewById(R.id.categoryicon);
            holder.thumbnail = (ImageView) view.findViewById(R.id.thumbnailimg);
            holder.category = (TextView) view.findViewById(R.id.category);
            holder.next = (ImageView) view.findViewById(R.id.imagenext);
            holder.distance = (TextView) view.findViewById(R.id.distance);
            holder.favorite = (ImageView) view.findViewById(R.id.favicon);
            holder.shortDescription = (TextView) view.findViewById(R.id.short_description);

            holder.uid = item.getUid();
            view.setTag(holder);
        }
        return (PlaceHolder) view.getTag();
    }

    public static void fillPlaceRow(View view, final PlaceElement item, final Context context, boolean showDistance) {
        fillPlaceRow(getHolder(view, item), item, context, showDistance);
    }

    private static void fillPlaceRow(PlaceHolder holder, PlaceElement item, Context context, boolean showDistance) {


        boolean isListParent = Utils.equals(Controller.getInstance().getGroupFilter(), item.getUid());


        String title = item.getTitle();
        if (isListParent) {
            title += "\n("+context.getString(R.string.general_data)+")";
        }

        holder.title.setText(title);

        String shortTitle = item.getAttributes().get("shortdescription");
        //set status has short title
        String status=item.getAttributes().get("rt_status");
        if(!Utils.isEmpty(status))
        {
            shortTitle=status;
        }


        if (Utils.isEmpty(shortTitle))
        {

            holder.shortDescription.setVisibility(View.INVISIBLE);
        } else {
            holder.shortDescription.setText(shortTitle);
            holder.shortDescription.setVisibility(View.VISIBLE);
        }


        // int res_id=context.getResources().getIdentifier("category_"+item.getCategory(), "drawable", "org.gmu.activities");

        String mainImg = AbstractPlaceElementDAO.getMainImg(item);
        if (!Utils.isEmpty(mainImg)) {
            Controller.getInstance().getDao().loadImageInView(holder.thumbnail, mainImg, Constants.PLACE_THUMBNAILDPI);

        } else {
            holder.thumbnail.setImageResource(R.drawable.default_img);
        }


        boolean isParent = Controller.getInstance().getDao().isParentPlaceElement(item);
        if (isListParent || !isParent) {
            holder.next.setVisibility(View.INVISIBLE);

        } else {
            holder.next.setVisibility(View.VISIBLE);


        }
        if (Utils.contains(IPlaceElementDAO.DETAIL_ROOT_ITEMS, item.getType())) {
            holder.favorite.setVisibility(View.VISIBLE);
            holder.icon.setVisibility(View.VISIBLE);
            Controller.getInstance().getDao().loadCategoryIconInView(holder.icon, item);
            holder.category.setVisibility(View.VISIBLE);
            CategoryTranslationTable.setAttributeTranslation(context,holder.category,item.getCategory());

        } else {
            holder.favorite.setVisibility(View.GONE);
            holder.icon.setVisibility(View.GONE);
            holder.category.setVisibility(View.GONE);
            showDistance = false;
            holder.distance.setVisibility(View.INVISIBLE);
        }

        if (item.getType().equals(PlaceElement.TYPE_ROUTE)) {
            showDistance = false;
            //in routes: show duration if exist
            String duration = item.getAttributes().get("duration");
            if (!Utils.isEmpty(duration)) {
                holder.distance.setText(duration);
                holder.distance.setVisibility(View.VISIBLE);
            }else
            {
                holder.distance.setVisibility(View.INVISIBLE);
            }
        }else if(!showDistance)
        {   showDistance=false;
            holder.distance.setVisibility(View.INVISIBLE);

        }



        //set distance if location is over world map
        if (showDistance && Utils.isEmpty(Controller.getInstance().getDao().getFixedMap())) {


            //update distance

            Float distance = item.getDistanceToUser();
            if (Utils.isEmpty(item.getAttributes().get("indoor_map")) && distance != null) {

//                Location l = MapUtils.WKT2Location(item.getPointWKT());
//                distance = l.distanceTo(Controller.getInstance().getGmuContext().lastUserLocation);
                holder.distance.setText(Utils.formatDistance(item.getDistanceToUser()));
                holder.distance.setVisibility(View.VISIBLE);
            } else

            {   //hide distance on indoor places
                holder.distance.setVisibility(View.INVISIBLE);
            }


        }


        final String uid = item.getUid();
        boolean isFavorite = item.getAttributes().get("favorite") != null;
        final ImageView favorite = holder.favorite;
        if (!isFavorite) {
            favorite.setImageResource(R.drawable.ic_action_favorite_off);
        } else {
            favorite.setImageResource(R.drawable.ic_action_favorite_on);
        }

        favorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean isfavo = Controller.getInstance().getDao().load(uid).getAttributes().containsKey("favorite");


                if (isfavo) {
                    favorite.setImageResource(R.drawable.ic_action_favorite_off);

                } else {
                    favorite.setImageResource(R.drawable.ic_action_favorite_on);

                }
                Controller.getInstance().setFavorite(uid, isfavo);
            }
        });


    }


    private static class PlaceHolder {
        TextView title;

        ImageView icon;
        ImageView thumbnail;
        ImageView next;
        TextView distance;
        TextView category;
        ImageView favorite;
        TextView shortDescription;

        String uid;
    }


}
