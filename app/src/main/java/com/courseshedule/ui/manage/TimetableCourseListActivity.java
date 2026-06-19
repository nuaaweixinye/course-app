package com.courseshedule.ui.manage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.CourseEntity;
import com.courseshedule.databinding.ActivityTimetableCourseListBinding;
import com.courseshedule.ui.course.CourseEditActivity;

import java.util.List;

public class TimetableCourseListActivity extends AppCompatActivity {

    public static final String EXTRA_TIMETABLE_ID = "timetable_id";
    public static final String EXTRA_TIMETABLE_NAME = "timetable_name";

    private ActivityTimetableCourseListBinding binding;
    private CourseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTimetableCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long timetableId = getIntent().getLongExtra(EXTRA_TIMETABLE_ID, -1L);
        String timetableName = getIntent().getStringExtra(EXTRA_TIMETABLE_NAME);
        if (timetableId < 0) { finish(); return; }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (timetableName != null) {
            binding.toolbar.setTitle(getString(R.string.timetable_course_list_title, timetableName));
        }

        adapter = new CourseAdapter();
        binding.courseList.setLayoutManager(new LinearLayoutManager(this));
        binding.courseList.setAdapter(adapter);

        AppDatabase db = ((App) getApplication()).getDatabase();
        db.courseDao().observeByTimetable(timetableId).observe(this, courses -> {
            if (courses == null || courses.isEmpty()) {
                binding.courseList.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
            } else {
                binding.courseList.setVisibility(View.VISIBLE);
                binding.emptyView.setVisibility(View.GONE);
                adapter.setCourses(courses);
            }
        });
    }

    private class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {
        private List<CourseEntity> courses;

        void setCourses(List<CourseEntity> list) {
            courses = list;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int i) {
            CourseEntity c = courses.get(i);
            h.text.setText(c.name);
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(TimetableCourseListActivity.this, CourseEditActivity.class);
                intent.putExtra(CourseEditActivity.EXTRA_COURSE_ID, c.id);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return courses == null ? 0 : courses.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(@NonNull View v) {
                super(v);
                text = v.findViewById(android.R.id.text1);
            }
        }
    }
}
