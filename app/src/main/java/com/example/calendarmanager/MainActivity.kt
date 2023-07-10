package com.example.calendarmanager

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener


class MainActivity : AppCompatActivity() {
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var toggleButton: ToggleButton

    private var taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendarView = findViewById(R.id.calendarView)
        toggleButton = findViewById(R.id.toggleButton)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Chế độ hiển thị tuần
                setCalendarDisplayModeWeeks()
            } else {
                // Chế độ hiển thị tháng
                setCalendarDisplayModeMonths()
            }
        }

        loadTaskList()

        setColorDate()

        calendarView.setOnDateChangedListener(object : OnDateSelectedListener {
            override fun onDateSelected(
                widget: MaterialCalendarView,
                date: CalendarDay,
                selected: Boolean
            ) {
                showTaskDialog(date)
            }
        })
    }

    private fun setCalendarDisplayModeWeeks() {
        calendarView.state().edit()
            .setCalendarDisplayMode(CalendarMode.WEEKS)
            .commit()
        setColorDate()
    }

    private fun setCalendarDisplayModeMonths() {
        calendarView.state().edit()
            .setCalendarDisplayMode(CalendarMode.MONTHS)
            .commit()
        setColorDate()
    }

    private fun setColorDate() {
        val colorTaskPending = Color.BLUE
        val colorTaskCompleted = Color.GREEN
        val colorToday = resources.getColor(R.color.colorAccent)
        val colorPast = resources.getColor(R.color.colorPast)
        val colorFuture = resources.getColor(R.color.colorFuture)

        // Thiết lập màu sắc cho ngày hôm nay
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                return day == CalendarDay.today()
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(ForegroundColorSpan(colorToday))
                view.addSpan(StyleSpan(Typeface.BOLD))
            }
        })

        // Thiết lập màu sắc cho ngày quá khứ
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                return day.isBefore(CalendarDay.today())
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(ForegroundColorSpan(colorPast))
            }
        })

        // Thiết lập màu sắc cho ngày tương lai
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                return day.isAfter(CalendarDay.today())
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(ForegroundColorSpan(colorFuture))
            }
        })

        // Thiết lập màu sắc cho ngày chưa hoàn thành
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                val tasks = getTasksForDate(day)
                return tasks.any { !it.isDone }
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(ForegroundColorSpan(colorTaskPending))
            }
        })

        // Thiết lập màu sắc cho ngày đã hoàn thành
        calendarView.addDecorator(object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                val tasks = getTasksForDate(day)
                return tasks.isNotEmpty() && tasks.all { it.isDone }
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(ForegroundColorSpan(colorTaskCompleted))
            }
        })
    }

    private fun showTaskDialog(date: CalendarDay) {
        val tasks = getTasksForDate(date)

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_task, null)
        dialogBuilder.setView(dialogView)

        val editTextTaskContent = dialogView.findViewById<EditText>(R.id.editTextTaskContent)
        val checkboxLayout = dialogView.findViewById<LinearLayout>(R.id.checkboxLayout)

        dialogBuilder.setTitle("Danh sách công việc")

        if (tasks.isNotEmpty()) {
            var selectedTaskIndex = -1
            for ((i, task) in tasks.withIndex()) {
                val taskLayout = LinearLayout(this)
                taskLayout.orientation = LinearLayout.HORIZONTAL

                val checkBox = CheckBox(this)
                checkBox.text = task.content
                checkBox.isChecked = task.isDone
                val checkBoxParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                checkBoxParams.weight = 1f
                checkBox.layoutParams = checkBoxParams
                taskLayout.addView(checkBox)

                val editTextView = TextView(this)
                editTextView.text = "Sửa"
                editTextView.setTextColor(Color.parseColor("#7E027E")) // Màu tím
                val editTextViewParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                editTextViewParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                editTextViewParams.marginEnd = resources.getDimensionPixelSize(R.dimen.edit_text_margin_end)
                editTextView.layoutParams = editTextViewParams
                taskLayout.addView(editTextView)

                val deleteTextView = TextView(this)
                deleteTextView.text = "Xóa"
                deleteTextView.setTextColor(Color.parseColor("#7E027E")) // Màu tím
                val deleteTextViewParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                deleteTextViewParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                deleteTextViewParams.marginEnd = resources.getDimensionPixelSize(R.dimen.delete_text_margin_end)
                deleteTextView.layoutParams = deleteTextViewParams
                taskLayout.addView(deleteTextView)

                checkboxLayout.addView(taskLayout)

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    task.isDone = isChecked

                    saveTaskList()

                    setColorDate()
                }

                editTextView.setOnClickListener {
                    editTextTaskContent.setText(task.content)
                    selectedTaskIndex = i
                }

                deleteTextView.setOnClickListener {
                    removeTaskForDate(date, task)
                    checkboxLayout.removeView(taskLayout)
                    setColorDate()
                }
            }

            dialogBuilder.setNeutralButton("Lưu") { _, _ ->
                if (editTextTaskContent.text.toString().isNotEmpty()) {
                    val taskContent = editTextTaskContent.text.toString()
                    val selectedTask = tasks[selectedTaskIndex]
                    selectedTask.content = taskContent

                    saveTaskList()

                    setColorDate()
                }
            }
        }

        dialogBuilder.setNegativeButton("Hủy", null)

        dialogBuilder.setPositiveButton("Thêm") { _, _ ->
            if (editTextTaskContent.text.toString().isNotEmpty()) {
                val taskContent = editTextTaskContent.text.toString()
                val isTaskDone = false
                val newTask = Task(date, taskContent, isTaskDone)
                addTaskForDate(date, newTask)
                setColorDate()
            }
        }

        val dialog = dialogBuilder.create()

        dialog.show()
    }


    private fun getTasksForDate(date: CalendarDay): List<Task> {
        val tasksForDate = mutableListOf<Task>()
        for (task in taskList) {
            if (task.date == date) {
                tasksForDate.add(task)
            }
        }
        return tasksForDate
    }

    private fun addTaskForDate(date: CalendarDay, newTask: Task) {
        newTask.date = date
        taskList.add(newTask)

        saveTaskList()
    }

    private fun removeTaskForDate(date: CalendarDay, task: Task) {
        taskList.remove(task)

        saveTaskList()
    }

    private fun saveTaskList() {
        val sharedPreferences = getSharedPreferences("TaskListPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(taskList)
        editor.putString("taskList", json)
        editor.apply()
    }

    private fun loadTaskList() {
        val sharedPreferences = getSharedPreferences("TaskListPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("taskList", null)
        val type = object : TypeToken<MutableList<Task>>() {}.type
        taskList = gson.fromJson(json, type) ?: mutableListOf()
    }
}
