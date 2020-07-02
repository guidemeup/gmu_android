package org.gmu.fragments.placedetails;


import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.gmu.config.Constants;
import org.gmu.base.R;
import org.gmu.context.GmuContext;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.SchemaDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.translation.AttributesTranslationTable;
import org.gmu.ui.GMUImageGetter;
import org.gmu.ui.GenericPlaceScrollView;
import org.gmu.utils.Utils;
import org.gmu.utils.image.ImageDescriptor;
import org.gmu.utils.image.ImageUtils;

import java.io.File;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * Generic Fragment for a place, renders place and ignores children
 */
public class GenericPlaceDetailFragment extends AbstractPlaceDetailFragment
{
    private static final String TAG = GenericPlaceDetailFragment.class.getName();
    private ImageView mImageView;
    private TextView mTitleView;
    private TextView mSubTitleView;
    private LinearLayout baseView;
    private GenericPlaceScrollView scrollView;
    private String title;

    private PlaceElement placeElement;
    private boolean showTitle = true;
    private boolean showMainImg = true;
    private double imageRatio = Constants.HEIGHT_PLACE_DETAIL_RATIO;
    private GMUImageGetter imgGetter=new GMUImageGetter();

    public GenericPlaceDetailFragment() {
        super();

    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(false);


    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        resume(savedInstanceState);

        Log.d(TAG, "Create detail of:" + UID);
        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.fragment_genericplaceview,
                container, false);




        mTitleView = (TextView) main.findViewById(R.id.title);
        mSubTitleView   = (TextView) main.findViewById(R.id.shorttitle);
        mImageView = (ImageView) main.findViewById(R.id.imageView);


        mImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {   //get image uri and open in external image viewer

                String uri = Controller.getInstance().getDao().getRelatedFileDescriptor(placeElement.getAttributes().get("main_img"));
                File file = new File(uri);

//                Intent i = new Intent(GenericPlaceDetailFragment.this.getActivity(), SimpleImageViewerActivity.class);
//                i.putExtra("filepath", file.getAbsolutePath());
//                startActivity(i);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);

                intent.setDataAndType(Uri.fromFile(file), "image/*");
                startActivity(intent);


            }
        });

        ImageButton play = (ImageButton) main.findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {   //get image uri and open in external image viewer

                String uri = Controller.getInstance().getDao().getRelatedFileDescriptor(placeElement.getAttributes().get("related_file"));
                File file = new File(uri);

//                Intent i = new Intent(GenericPlaceDetailFragment.this.getActivity(), SimpleImageViewerActivity.class);
//                i.putExtra("filepath", file.getAbsolutePath());
//                startActivity(i);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);

                intent.setDataAndType(Uri.fromFile(file), "audio/*");
                startActivity(intent);


            }
        });





        baseView = (LinearLayout) main.findViewById(R.id.baseview);
        scrollView=(GenericPlaceScrollView) main.findViewById(R.id.scrollView);
        return main;
    }

    public void onResume() {
        super.onResume();
        Log.d(TAG," Resumed detail fragment="+UID);

        //TODO: refresh on resume (no hay manera de hacer que se refresque al restaurar instancia con el pager)!!
        show(UID);
    }

    public void onDestroyView()
    {
        super.onDestroyView();


        Utils.unbindDrawables(baseView);
        //Utils.unbindDrawables( myGallery);
        baseView = null;
        mImageView = null;
        mTitleView = null;
        System.gc();
        Runtime.getRuntime().gc();
        Log.d(TAG, "Destroyed detail of:" + this.getTitle());

    }

    public void showMainImg(boolean show) {
        showMainImg = show;
    }

    public void setImageRatio(double imageRatio) {
        this.imageRatio = imageRatio;
    }

    public void showTitle(boolean show) {
        showTitle = show;
    }

    public String getTitle() {
        return title;
    }

    public void show(String UID)
    {
        //calculate main image w and h
        DisplayMetrics dm = new DisplayMetrics();
        this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenH = dm.heightPixels;
        int screenW = dm.widthPixels;
        int minScreenSize=screenW;
        //adjust image size to portrait mode
        if(screenW>screenH)
        {   //invert

            minScreenSize=screenH;


        }

        int targetH = (int) (minScreenSize * imageRatio);





        Log.d(TAG, "Search " + UID);
        placeElement = Controller.getInstance().getDao().load(UID);

        if (placeElement == null) {
            Log.d(TAG, "Show uid null?" + UID);
            return;
        }
        if (showTitle)
        {
            mTitleView.setVisibility(View.VISIBLE);
            String subtitle=placeElement.getAttributes().get("shortdescription");
            if(!Utils.isEmpty(subtitle))
            {  mSubTitleView.setVisibility(View.VISIBLE);
                mSubTitleView.setText(subtitle);
            }else
            { mSubTitleView.setVisibility(View.GONE);

            }

        } else
        {
            mTitleView.setVisibility(View.GONE);
            mSubTitleView.setVisibility(View.GONE);

        }



        title = placeElement.getTitle();


        mTitleView.setText(title);
        baseView.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) this.getActivity().getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        boolean someData = false;

        //check if there's more than one header
        boolean onlyDescriptionHeader = true;



        List<String> targetHeaders=new LinkedList<String>();

        String[] headers= SchemaDAO.loadSchemaVisibility(placeElement.getType(), placeElement.getAttributes().get("schema"));

        for (int i = 0; i < headers.length; i++)
        {
            String header = headers[i];
            String data = placeElement.getAttributes().get(header);
            if (Utils.isEmpty(data)) continue;
            if (i != 0)
            {
                onlyDescriptionHeader = false;
            }
            targetHeaders.add( header);
        }
        //add extra attribs
        Set<String> att= placeElement.getAttributes().keySet();
        for (Iterator<String> iterator = att.iterator(); iterator.hasNext(); ) {
            String name = iterator.next();
            if(name.startsWith(Constants.SPECIALATTRIBS_PREFIX))
            {
                targetHeaders.add( name);
                onlyDescriptionHeader = false;
            }

        }

        for (int i = 0; i < targetHeaders.size(); i++)
        {
            String header =  targetHeaders.get(i);
            String data = placeElement.getAttributes().get(header);
            if (Utils.isEmpty(data)) continue;


            final View v2 = inflater.inflate(R.layout.detail_data, null);



            TextView dataV = (TextView) v2.findViewById(R.id.detailtext);
            data = data.trim();
            //add html gallery at the end
            //TODO: code native gallery
            if( header.equalsIgnoreCase("description")&&SchemaDAO.showGallery(placeElement.getAttributes().get("schema")))
            {

                List<PlaceElement> found = Controller.getInstance().getDao().list(null, placeElement.getUid(),
                        IPlaceElementDAO.PLACE_RELATED_ITEMS, 1, null);
                if(found.size()>0)
                {
                    StringBuilder ret=new StringBuilder("<p>");
                    for (int j = 0; j < found.size(); j++)
                    {
                        PlaceElement element = found.get(j);
                        ret.append("<a href=\""+element.getAttributes().get("url")+"\"><img  alt=\"img\"  src=\""+element.getAttributes().get("url")+"\" /></a><br></br>" );


                    }
                    ret.append("</p>");
                    data=data+ret.toString();
                }


            }





            int fullWidth= screenW-(baseView.getPaddingLeft()+baseView.getPaddingRight()+v2.getPaddingLeft()+v2.getPaddingRight());
            imgGetter.setWidth(fullWidth);
            imgGetter.setImgWidth(minScreenSize);
            imgGetter.setContext(this.getActivity());
            imgGetter.setIgnoredResource(placeElement.getAttributes().get("main_img"));
            Utils.fillFromHtml(data, this.getActivity(), dataV, imgGetter);




            if (!onlyDescriptionHeader)
            {
                View v1 = inflater.inflate(R.layout.detail_header, null);
                TextView headerV = (TextView) v1.findViewById(R.id.separator);
                AttributesTranslationTable.setAttributeTranslation(this.getActivity(),headerV, header.replace(Constants.SPECIALATTRIBS_PREFIX, ""));

                baseView.addView(v1);
                v1.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        Utils.toggleVisibility(v2, !(v2.getVisibility() == View.VISIBLE));


                    }
                });

            }

            baseView.addView(v2);
            if(someData)
            {   //toggle others
               // v2.setVisibility(View.INVISIBLE);
            }


            someData = true;
        }


        if (!someData) {   //set full screen on image

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(screenW, screenH);
            mImageView.setLayoutParams(layoutParams);
            mImageView.setScaleType(ImageView.ScaleType.FIT_START);


        } else
        {
                 //default image layout: crop vertically

                //image ratio
                //int target = (int)(((double)screenW)*Constants.MAIN_IMG_RATIO);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(minScreenSize, targetH);
                //center image
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                mImageView.setLayoutParams(layoutParams);


        }
        if (showMainImg && !Utils.isEmpty(placeElement.getAttributes().get("main_img")))
        {
            if( !SchemaDAO.mustCropVertically(placeElement.getAttributes().get("schema")))
            {
                //don t crop  vertically
                 try
                 {   BitmapFactory.Options options= ImageUtils.readImageMetadata(Controller.getInstance().getDao().getRelatedFileDescriptor( placeElement.getAttributes().get("main_img")));
                     targetH = (options.outHeight*minScreenSize) / options.outWidth;
                     RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(minScreenSize, targetH);
                     //center image
                     layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                     layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                     mImageView.setLayoutParams(layoutParams);


                 } catch (Exception ign)
                 {
                    Log.w(TAG,"Image not found?");

                 }


            }
            Controller.getInstance().getDao().loadImageInView(mImageView, placeElement.getAttributes().get("main_img"), Utils.convertPixelToDp(screenW, this.getActivity()));
        } else {
            this.getView().findViewById(R.id.main_img_area).setVisibility(View.GONE);
        }



        if (!Utils.isEmpty(placeElement.getAttributes().get("mime")) && placeElement.getAttributes().get("mime").startsWith(PlaceElement.MIME_AUDIO)) {
            this.getView().findViewById(R.id.play).setVisibility(View.VISIBLE);

        } else {
            this.getView().findViewById(R.id.play).setVisibility(View.GONE);
        }
        if(placeElement!=null&&placeElement.getType().equalsIgnoreCase(PlaceElement.TYPE_MULTIMEDIA))
        {        //get icon  id and load
                 String category= getOrderInRouteIcon();
                 if(category!=null)
                 {   String initialCategory = placeElement.getCategory();
                     placeElement.setCategory(category);
                     Controller.getInstance().getDao().loadCategoryIconInView(((ImageView) this.getView().findViewById(R.id.child_icon)),placeElement);
                     placeElement.setCategory(initialCategory);
                     ((ImageView) this.getView().findViewById(R.id.child_icon)).setVisibility(View.VISIBLE);
                 }

        } else
        {
            ((ImageView) this.getView().findViewById(R.id.child_icon)).setVisibility(View.GONE);
        }

        //restore scroll
        scrollView.setPlaceUID(UID);
        final Integer sIndex=Controller.getInstance().getScrollIndex(UID) ;
        if(sIndex!=null&&sIndex>0)
        {
            scrollView.post(new Runnable() {

                public void run() {
                    scrollView.scrollTo(0, sIndex);

                }
            });
        }



    }


    private String getOrderInRouteIcon() {
        PlaceElement selected = Controller.getInstance().getDao().load(Controller.getInstance().getSelectedUID());

        boolean isRoute = selected.getType().equals(PlaceElement.TYPE_ROUTE);
        if (isRoute) {
            List<PlaceElement> relatedObjects = Controller.getInstance().getDao().getRelatedPlaceElements(selected.getUid(), IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS, IPlaceElementDAO.PLACE_RELATED_ITEMS);

            int routeCounter = 1;
            for (int i = 0; i < relatedObjects.size(); i++) {
                PlaceElement elem = relatedObjects.get(i);

                if (!Utils.isEmpty(elem.getPointWKT())) {
                    String o = "00" + routeCounter;
                    o = o.substring(o.length() - 2);

                    if (placeElement.getUid().equalsIgnoreCase(elem.getUid())) {
                        return o;
                    }
                    routeCounter++;

                }


            }


        }
        return null;
    }



}
