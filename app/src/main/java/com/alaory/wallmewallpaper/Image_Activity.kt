package com.alaory.wallmewallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import coil.ImageLoader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.ortiz.touchview.OnTouchImageViewListener
import com.ortiz.touchview.TouchImageView
import java.io.ByteArrayOutputStream
import java.io.File


class Image_Activity(): AppCompatActivity(){
    //ui elements
    private var titlePost: TextView? = null;
    private var auther_post: TextView? = null;
    private var url_post: TextView? = null;
    private var Full_image: TouchImageView? = null;
    private var mybitmap: Bitmap? = null;
    private var taggroup: ChipGroup? = null;


    private var setWallPaperButton : ImageButton? = null;
    private var saveWallpaperButton : ImageButton? = null;

     var myData : List_image? = null;
     var thumbnail: Drawable? = null;

    enum class mode{
        wallhaven,
        reddit
    }



    private  var cricle_prograssBar : ProgressBar? = null;
    //to check if the image is loaded
    private var loaded = false;

    companion object{
        //the clicked data by the user
         var MYDATA : List_image? = null;
         var THUMBNAIL: Drawable? = null;
        //save bitmap to file and load it as a uri
        //mode
        var postmode = mode.reddit;
        //wallhaven tags
        var TagNameList : Array<String> = emptyArray();






        fun saveImage(context: Context, image: Bitmap,Name : String): String {
            val imageByteStream = ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG,100,imageByteStream);

            val path = MediaStore.Images.Media.insertImage(context.contentResolver,image,Name,null);
            return path;
        }





        fun Bitmap_toUri(context: Context, image: Bitmap): Uri? {
            var bytes = ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG,100,bytes);
            var imagepath = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"tempImage")
            if (!imagepath.mkdirs()) {
                Log.e("Image_Activity", "Directory not created")
            }
            var imagesaved = File(imagepath.absolutePath + "/temp.png");
            imagesaved.writeBytes(bytes.toByteArray());

            return Uri.parse(imagesaved.path);
        }

    }





    private fun setWallpaper(width : Int, height : Int){
        //set the wallpaper
        if(loaded){
//            try {
//                val uri_image = Bitmap_toUri(applicationContext,mybitmap!!);
//                val intent = Intent(Intent.ACTION_ATTACH_DATA);
//                intent.addCategory((Intent.CATEGORY_DEFAULT));
//                intent.setDataAndType(uri_image,"image/*");
//                intent.putExtra("mimeType","image/*")
//                this.startActivity(Intent.createChooser(intent,"Set as:"));
//            }catch (e:Exception){
//                Log.e("Image_Activity",e.toString())
//            }
            try{
                val screenWidth = resources.displayMetrics.widthPixels;
                val screenHeight = resources.displayMetrics.heightPixels;

                val wallpapermanager = WallpaperManager.getInstance(this);
                wallpapermanager.suggestDesiredDimensions(screenWidth,screenHeight);


                val scaledWidth = if(mybitmap!!.width > resources.displayMetrics.widthPixels) mybitmap!!.width else resources.displayMetrics.widthPixels;
                val scaledHeight = if(mybitmap!!.height > resources.displayMetrics.heightPixels) mybitmap!!.height else resources.displayMetrics.heightPixels;

                val MyeditedWallpaper : Bitmap = Bitmap.createScaledBitmap(mybitmap!!,scaledWidth,scaledHeight,false);


                var minWidth = if(width > resources.displayMetrics.widthPixels) resources.displayMetrics.widthPixels else width;
                var minHeight = if(height > resources.displayMetrics.heightPixels) resources.displayMetrics.heightPixels else height;

                var startWidth: Int = 0;

                if(Configuration.ORIENTATION_LANDSCAPE == Resources.getSystem().configuration.orientation)
                    minHeight = resources.displayMetrics.heightPixels;
                else{
                    startWidth = minWidth
                    minWidth = resources.displayMetrics.widthPixels;
                }


                val WallPaperBitmap : Bitmap = Bitmap.createBitmap(MyeditedWallpaper,startWidth,0,minWidth,minHeight);



                wallpapermanager.setBitmap(WallPaperBitmap);
                Toast.makeText(this,"Image has been set as Wallpaper :)",Toast.LENGTH_LONG).show();

            }catch (e:Exception){
                Log.e("Image_Activity",e.toString())
           }

        }else{
            Toast.makeText(this,"Please wait for the image to load",Toast.LENGTH_LONG).show();
        }
    }






    private fun HideSystemBar(){
        if(Configuration.ORIENTATION_LANDSCAPE == Resources.getSystem().configuration.orientation){
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ){
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        }
    }





    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle);
        //activity system and app bar
        this.supportActionBar!!.hide();
        HideSystemBar();
        //update screen orein
        MainActivity.checkorein();

        myData = MYDATA!!
        thumbnail = THUMBNAIL!!
        //set the activit as a main screen
        setContentView(R.layout.show_image_fs);

        //-----------------------------------------------------------------
        //bottom sheet
        val bottomsheetfragment = findViewById<FrameLayout>(R.id.ImageInfo_BottomSheet);
        BottomSheetBehavior.from(bottomsheetfragment).apply {
            peekHeight = 300;
            this.state = BottomSheetBehavior.STATE_COLLAPSED;



            val Views = LayoutInflater.from(this@Image_Activity).inflate(R.layout.bottom_sheet,null);
            bottomsheetfragment.addView(Views);

            //set bottom sheet info
            taggroup = Views.findViewById(R.id.TagGroup);
            taggroup!!.isVisible = false;
            titlePost = Views.findViewById(R.id.title_post);
            auther_post = Views.findViewById(R.id.auther_post);
            url_post = Views.findViewById(R.id.url_post);
            setWallPaperButton = Views.findViewById(R.id.set_imageButton);
            saveWallpaperButton = Views.findViewById(R.id.save_imageButton);


            //pull buttonimage
            Views.findViewById<ImageButton>(R.id.pullbottom).setOnClickListener {
                if(this.state == BottomSheetBehavior.STATE_EXPANDED)
                    this.state = BottomSheetBehavior.STATE_COLLAPSED;
                else
                    this.state = BottomSheetBehavior.STATE_EXPANDED;

                it.animate().apply {
                    duration = 300;

                    rotationXBy(180f);
                }
            }




        }

        //----------------------------------------------------


        //set the ui elements
        Full_image = findViewById(R.id.full_image);
        cricle_prograssBar = findViewById(R.id.cricle_prograssBar_FullImage);

        //set text for image info
        titlePost!!.setText(myData?.Image_title);
        auther_post!!.setText("posted by: ${myData?.Image_auther}");
        url_post!!.setText(myData?.post_url)




        //-----------------------------------------------------------------

        //set wallhaven post info with tag functionality
        if(postmode == mode.wallhaven){

            wallhaven_api.imageInfo(myData!!) {
                runOnUiThread {
                    taggroup!!.isVisible = true;
                    auther_post!!.setText("posted by: ${myData?.Image_auther}");
                    for (i in TagNameList){
                        val TagChip = LayoutInflater.from(this).inflate(R.layout.tagchip,taggroup,false) as Chip;
                        TagChip.text = i;

                        TagChip.setOnClickListener {

                            try {
                                val tempTag = wallhaven_api.Tag("&q=+$i");
                                val intent = Intent(this, TagActivity::class.java);
                                intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                TagActivity.Tag_Assing = tempTag;
                                startActivity(intent);
                            }catch (e: Exception){
                                Log.e("Reddit_posts","error while trying to set image activity")
                            }
                        }
                        taggroup!!.addView(TagChip)
                    }
                }
            }
            titlePost!!.isVisible = false;
        }

        //------------------------------------------------------------------
        Full_image!!.setOnTouchImageViewListener(object : OnTouchImageViewListener{
            override fun onMove() {
                Log.i("Full_image","width:${(Full_image!!.zoomedRect.width() * Full_image!!.width).toInt() } height:${(Full_image!!.zoomedRect.height() * Full_image!!.height).toInt()}");
            }
        })

        //load local bitmap and ui imageview data and do it in a callback
        ImageLoader(applicationContext).enqueue(coil.request.ImageRequest.Builder(this)
            .data(myData?.Image_url)
            .placeholder(thumbnail)
            .target(object : coil.target.Target{
                override fun onError(error: Drawable?) {
                    super.onError(error)
                    mybitmap = error!!.toBitmap();
                    Full_image!!.setImageBitmap(mybitmap);
                }

                override fun onStart(placeholder: Drawable?) {
                    super.onStart(placeholder)
                    mybitmap = placeholder!!.toBitmap();
                    Full_image!!.setImageBitmap(mybitmap);
                }

                override fun onSuccess(result: Drawable) {
                    super.onSuccess(result)
                    loaded = true;
                    mybitmap = result.toBitmap();
                    Full_image!!.setImageBitmap(mybitmap);
                }
            })
            .listener(
                onSuccess = {_,_ ->
                    cricle_prograssBar?.visibility = View.INVISIBLE;
                },
                onCancel = {
                    cricle_prograssBar?.visibility = View.INVISIBLE;
                },
                onError = {_,_ ->
                    cricle_prograssBar?.visibility = View.INVISIBLE;
                },
                onStart = {
                    cricle_prograssBar?.visibility = View.VISIBLE;
                }

            )
            .build()
        );

        //--------------------------------------------------------------------------
        //set the wallpaper set button
        setWallPaperButton?.setOnClickListener {
            setWallpaper(
                (Full_image!!.zoomedRect.width() * Full_image!!.width).toInt(),
                (Full_image!!.zoomedRect.height() * Full_image!!.height).toInt()
            );
        };


        saveWallpaperButton?.setOnClickListener {
            if(loaded) {
                saveImage(applicationContext,mybitmap!!, myData!!.Image_name);
                Toast.makeText(this,"Image has been saved to your photos directory",Toast.LENGTH_LONG).show();
            } else{
                Toast.makeText(this,"Please wait for the image to load",Toast.LENGTH_LONG).show();
            }
        };

        Log.i("Image_Activity","Info url ${myData?.Image_url} name ${myData?.Image_name} thumbnail ${myData?.Image_thumbnail}");
        //------------------------------------------------------------------------------

    }


}