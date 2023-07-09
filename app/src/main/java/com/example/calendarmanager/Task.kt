package com.example.calendarmanager

import com.prolificinteractive.materialcalendarview.CalendarDay

data class Task(
    var date: CalendarDay,
    var content: String,
    var isDone: Boolean
)
