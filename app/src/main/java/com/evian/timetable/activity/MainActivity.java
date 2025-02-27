package com.evian.timetable.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bigkoo.pickerview.builder.OptionsPickerBuilder;
import com.bigkoo.pickerview.listener.OnOptionsSelectChangeListener;
import com.bigkoo.pickerview.listener.OnOptionsSelectListener;
import com.bigkoo.pickerview.view.OptionsPickerView;

import com.evian.timetable.bean.Course;
import com.evian.timetable.R;
import com.evian.timetable.util.Config;
import com.evian.timetable.util.FileUtils;
import com.evian.timetable.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FrameLayout mFrameLayout;
    private TextView mWeekOfTermTextView;
    private ImageView mBgImageView;
    private ImageButton mAddImgBtn;

    public static List<Course> sCourseList;

    private static final int REQUEST_CODE_COURSE_DETAILS = 0;
    private static final int REQUEST_CODE_COURSE_EDIT = 1;
    private static final int REQUEST_CODE_CONFIG = 2;

    private OptionsPickerView mOptionsPv;

    private static final Map<String, Integer> mMap = new HashMap<String, Integer>() {{
        put("单周", 1);
        put("双周", 0);
        put("每周", 2);

    }};//判断是否为本周

    public static float VALUE_1DP;    //1dp的值

    private static final int CELL_HEIGHT = 70;  //课程视图的高度(px)
    private static float sCellWidthPx;          //课程视图的宽度(px)

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};   //读取权限

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWritePermission();   //读写权限用于保存课表信息

        int[] weekTextView = new int[]{     //储存周几表头
                R.id.tv_sun,
                R.id.tv_mon,
                R.id.tv_tues,
                R.id.tv_wed,
                R.id.tv_thur,
                R.id.tv_fri,
                R.id.tv_sat
        };
        mWeekOfTermTextView = findViewById(R.id.tv_week_of_term);   //课程详情页周数
        mAddImgBtn = findViewById(R.id.img_btn_add);                //图片按钮 添加课程
        mBgImageView = findViewById(R.id.iv_bg_main);               //主界面背景图片
        mFrameLayout = findViewById(R.id.fl_timetable);             //主界面课程面板

        Config.readFormSharedPreferences(this);             //读取当前周信息

        Utils.setPATH(getExternalFilesDir(null).getAbsolutePath() + File.separator + "pictures");//设置背景图片路径

        int headerClassNumWidth = findViewById(R.id.ll_header_class_num).getLayoutParams().width;   //课程表头空白部分宽度

        //计算1dp的数值方便接下来设置元素尺寸,提高效率
        VALUE_1DP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                getResources().getDisplayMetrics());

        //获取屏幕宽度，用于设置课程视图的宽度
        int displayWidth = getResources().getDisplayMetrics().widthPixels;

        sCellWidthPx = (displayWidth - headerClassNumWidth) / 7.0f;         //课程视图宽度

        int week = getWeekOfDay();  //获取星期几 星期天为1
        //Log.d("week", "" + week);
        updateCurrentWeek(week);    //判断是否是周一 如果是 周数加1

        TextView weekTv=findViewById(weekTextView[week-1]);     //获取当前周几的表头
        weekTv.setBackground(getDrawable(R.color.day_of_week_color));   //设置当前周几的表头高亮

        //设置标题为自定义toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        initFrameLayout();
        initTimetable();
        initAddBtn();

        Utils.setBackGround(this,mBgImageView);

    }

    @SuppressLint("ClickableViewAccessibility")
    private void initFrameLayout()
    {
        mFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int event=motionEvent.getAction();
                if(event==MotionEvent.ACTION_UP)
                {
                    if(mAddImgBtn.getVisibility()==View.VISIBLE)
                    {
                        mAddImgBtn.setVisibility(View.GONE);
                    }
                    else
                    {
                        int x=(int)motionEvent.getX();
                        int y=(int)motionEvent.getY();
                        x=x-x%(int) sCellWidthPx;
                        y=y-y%(int)(CELL_HEIGHT*VALUE_1DP);
                        setAddImgBtn(x,y);                  //显示添加课程图片按钮
                    }
                }
                return true;
            }
        });
    }

    //点击图片按钮跳转到添加课程活动 并传递当前星期和第几节课 用以初始化添加课程界面的时间
    private void initAddBtn() {
        final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mAddImgBtn.getLayoutParams();
        layoutParams.width = (int) (sCellWidthPx);
        mAddImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                int dayOfWeek=layoutParams.leftMargin/(int) sCellWidthPx;
                int classStart=layoutParams.topMargin/(int) (CELL_HEIGHT*VALUE_1DP);
                mAddImgBtn.setVisibility(View.INVISIBLE);
                intent.putExtra(EditActivity.EXTRA_Day_OF_WEEK,dayOfWeek+1);
                intent.putExtra(EditActivity.EXTRA_CLASS_START,classStart+1);
                startActivityForResult(intent, REQUEST_CODE_COURSE_EDIT);
            }
        });
    }

    //点击空白处添加上图片按钮
    private void setAddImgBtn(int left,int top)
    {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mAddImgBtn.getLayoutParams();
        layoutParams.leftMargin = left;
        layoutParams.topMargin=top;
        mAddImgBtn.setVisibility(View.VISIBLE);
    }

    /**
     * @return 今天是周几
     */
    private int getWeekOfDay() {
        //周日为一个星期的第一天，数值为1-7
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 启动应用时进行当前周数的更新,
     * 不足：不能在每周一00:00准时更新数据，需要依靠用户启动来实现更新
     */
    private void updateCurrentWeek(int week) {
        if (week == 2)  //判断是否为周一，周一更新当前周数
        {
            if (!Config.isFlagCurrentWeek())//利用flag实现周一只更新一次
            {
                Config.currentWeekAdd();
                Config.setFlagCurrentWeek(true);
                Config.saveSharedPreferences(this);
            }
        } else {
            if (Config.isFlagCurrentWeek()) {
                Config.setFlagCurrentWeek(false);
                Config.saveSharedPreferences(this);
            }
        }
    }

    //右上角设置菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.menu_config:      //菜单设置
                intent = new Intent(this, ConfigActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CONFIG);
                break;
            case R.id.menu_set_week:    //菜单设置当前周
                showSelectCurrentWeekDialog();
                break;
            case R.id.menu_append_class://菜单添加课程
                intent = new Intent(this, EditActivity.class);
                startActivityForResult(intent, REQUEST_CODE_COURSE_EDIT);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示周数列表,让用户从中选择
     */
    private void showSelectCurrentWeekDialog() {
        //String[] items = new String[25];
        final int currentWeek = Config.getCurrentWeek();
        final String str = "当前周为：";
        final List<String> items = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            items.add("第" + (i + 1) + "周");
        }

        mOptionsPv = new OptionsPickerBuilder(this, new OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) {
                int week = options1 + 1;
                if (options1 != currentWeek) {
                    Config.setCurrentWeek(week);
                    updateTimetable();
                    Config.saveSharedPreferences(MainActivity.this);
                }
            }
        }).setOptionsSelectChangeListener(new OnOptionsSelectChangeListener() {
            @Override
            public void onOptionsSelectChanged(int options1, int options2, int options3) {
                mOptionsPv.setTitleText(str + items.get(options1));
            }
        }).build();

        mOptionsPv.setTitleText("当前周为:" + items.get(currentWeek - 1));
        mOptionsPv.setNPicker(items, null, null);
        mOptionsPv.setSelectOptions(currentWeek - 1);
        mOptionsPv.show();
    }

    /**
     * 获取读写权限
     */

    private void getWritePermission() {
        try {       //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");

            if (permission != PackageManager.PERMISSION_GRANTED) {// 没有写的权限，去申请写的权限，会弹出对话框

                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化课表
     */
    private void initTimetable()//根据保存的信息，创建课程表
    {
        //设置标题中显示的当前周数
        mWeekOfTermTextView.setText(String.format(getString(R.string.day_of_week), Config.getCurrentWeek()));
        sCourseList = FileUtils.readFromJson(this);

        //读取失败返回
        if (sCourseList == null) {
            sCourseList = new ArrayList<>();
            return;
        }

        int size = sCourseList.size();
        if (size != 0) {
            updateTimetable();
        }
    }

    /**
     * 清空课程TextView
     */
    private void clearTimetable() {
        int count = mFrameLayout.getChildCount();
        for (int i = count - 1; i > 1; i--)
            mFrameLayout.removeViewAt(i);
    }

    /**
     * 更新课程表视图
     */
    private void updateTimetable() {

        //设置标题中显示的当前周数
        mWeekOfTermTextView.setText(String.format(getString(R.string.day_of_week), Config.getCurrentWeek()));

        List<Course> courseList = new ArrayList<>();

        boolean[] flag = new boolean[12];   //-1表示节次没有课程,其他代表占用课程的在mCourseList中的索引

        int weekOfDay = 0;

        int size = sCourseList.size();

        for (int index = 0; index < size; index++)//当位置有两个及以上课程时,显示本周上的课程,其他不显示
        {
            Course course = sCourseList.get(index);
            //Log.d("week", course.getDayOfWeek() + "");
            if (course.getDayOfWeek() != weekOfDay) {
                for (int i = 0; i < flag.length; i++) {
                    flag[i] = false;
                }
                weekOfDay = course.getDayOfWeek();  //在周几上课
            }

            int class_start = course.getClassStart();
            int class_num = course.getClassLength();

            int i;
            for (i = 0; i < class_num; i++) {
                if (flag[class_start + i - 1]) {
                    //Log.d("action", "if");
                    if (!courseIsThisWeek(course)) {
                        break;
                    } else {
                        courseList.remove(courseList.size() - 1);

                        courseList.add(course);
                        for (int j = 0; j < class_num; j++) {
                            flag[class_start + j - 1] = true;
                        }
                        break;
                    }
                }
            }
            if (i == class_num) {
                courseList.add(course);
                for (int j = 0; j < class_num; j++) {
                    flag[class_start + j - 1] = true;
                }
            }
        }

        clearTimetable();


        //int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CELL_HEIGHT, getResources().getDisplayMetrics());
        int height = (int) (CELL_HEIGHT * VALUE_1DP);

        size = courseList.size();

        StringBuilder stringBuilder = new StringBuilder();


        int[] color = new int[]{
                ContextCompat.getColor(this, R.color.item_orange),
                ContextCompat.getColor(this, R.color.item_tomato),
                ContextCompat.getColor(this, R.color.item_green),
                ContextCompat.getColor(this, R.color.item_cyan),
                ContextCompat.getColor(this, R.color.item_purple),
        };

        //Log.d("size", size + "");
        for (int i = 0; i < size; i++) {

            Course course = courseList.get(i);
            int class_num = course.getClassLength();
            int week = course.getDayOfWeek() - 1;
            int class_start = course.getClassStart() - 1;

            View view = initTextView(class_num, (int) (week * sCellWidthPx), class_start * height);

            TextView textView = view.findViewById(R.id.grid_item_text_view);

            setTableClickListener(textView, sCourseList.indexOf(course));

            String name = course.getName();
            if (name.length() > 10) {
                name = name.substring(0, 10) + "...";
            }
            stringBuilder.append(name);
            stringBuilder.append("\n@");
            stringBuilder.append(course.getClassRoom());

            GradientDrawable myGrad = new GradientDrawable();//动态设置TextView背景
            myGrad.setCornerRadius(5 * VALUE_1DP);

            if (courseIsThisWeek(course))//判断是否为当前周课程，如果不是，设置背景为灰色
            {
                myGrad.setColor(color[i % 5]);
                textView.setText(stringBuilder.toString());
            } else {
                myGrad.setColor(getResources().getColor(R.color.item_gray));
                stringBuilder.insert(0, "<small>[非本周]</small>\n");
                String str = stringBuilder.toString();
                str = str.replaceAll("\n", "<br />");
                textView.setText(Html.fromHtml(str));

            }

            textView.setBackground(myGrad);


            mFrameLayout.addView(view);

            stringBuilder.delete(0, stringBuilder.length());
        }

    }

    /**
     * 设置课程视图的监听
     *
     * @param textView
     * @param index
     */
    private void setTableClickListener(TextView textView, final int index)//设置课程视图的监听
    {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CourseDetailsActivity.class);
                intent.putExtra(CourseDetailsActivity.KEY_COURSE_INDEX, index);
                startActivityForResult(intent, REQUEST_CODE_COURSE_DETAILS);
            }
        });
    }

    /**
     * 初始化课程视图
     *
     * @param class_num 课程节数
     * @param left      左边距
     * @param top       上边距
     * @return 课程视图
     */
    private View initTextView(int class_num, final int left, final int top) {

        View view = getLayoutInflater().inflate(R.layout.item_timetable, mFrameLayout, false);

        TextView textView = view.findViewById(R.id.grid_item_text_view);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textView.getLayoutParams();
        layoutParams.width = (int) (sCellWidthPx - 6 * VALUE_1DP);


        if (class_num != 2) {
            layoutParams.height = (int) (VALUE_1DP * (CELL_HEIGHT * class_num - 6));//设置课程视图高度
        }

        layoutParams.topMargin = (int) (top + 3 * VALUE_1DP);
        layoutParams.leftMargin = (int) (left + 3 * VALUE_1DP);

        return view;
    }

    /**
     * @param course 课程
     * @return 是否为本周应该上的课程
     */
    private boolean courseIsThisWeek(Course course) {
        String class_week = course.getWeekOfTerm();     //应该上课的周
        if (class_week == null)
            return false;
        String[] strings = class_week.split(",");
        int currentWeek = Config.getCurrentWeek();
        for (String s : strings) {
            if (s.contains("-")) {          //处理x-y周格式
                String[] str = s.split("-");
                int start = Integer.parseInt(str[0]);
                int end = Integer.parseInt(str[1]);

                if (currentWeek >= start && currentWeek <= end) {
                    int i = mMap.get(course.getWeekOptions());
                    if (i == 2 || currentWeek % 2 == i)
                        return true;
                }
            } else {
                if (currentWeek == Integer.parseInt(s))
                    return true;
            }

        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;
        switch (requestCode) {
            case REQUEST_CODE_COURSE_EDIT:

            case REQUEST_CODE_COURSE_DETAILS:
                if (data == null)
                    return;
                boolean update = data.getBooleanExtra(EditActivity.EXTRA_UPDATE_TIMETABLE, false);
                if (update)
                    updateTimetable();
                break;

            case REQUEST_CODE_CONFIG:
                if (data == null)
                    return;
                boolean update_bg = data.getBooleanExtra(ConfigActivity.EXTRA_UPDATE_BG, false);
                if (update_bg)
                    Utils.setBackGround(this,mBgImageView);
                break;

            default:
                break;
        }
    }
}
