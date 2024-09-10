package com.example.diary

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class CalendarAdapter(
    private val daysOfMonth: List<String>,
    private val activity: FragmentActivity?,
    private val calendar: Calendar,
    private val datesWithSchedules: Set<String>,// 일정이 있는 날짜들
    private val dateRangesWithSchedules: MutableSet<String>,// 일정 범위가 설정된 날짜들
    private val emojiMap: MutableMap<String, Int>,
    private val onDateSelected: (String) -> Unit,// 날짜가 선택되었을 때 호출되는 콜백 함수
    private val onDateRangeSelected: (String, String) -> Unit,// 날짜 범위가 선택되었을 때 호출되는 콜백 함수
    private val onSaveDateRanges: () -> Unit// 일정 범위를 저장할 때 호출되는 콜백 함수
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val displayMetrics = parent.context.resources.displayMetrics
        val itemWidth = displayMetrics.widthPixels / 7

        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar, parent, false)
        itemView.layoutParams = ViewGroup.LayoutParams(itemWidth, itemWidth + 70)

        return CalendarViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = daysOfMonth[position]
        holder.dayTextView.text = day

        val dateKey = "${calendar.get(Calendar.YEAR)}.${calendar.get(Calendar.MONTH) + 1}.$day"
        val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false

        if (isDarkMode) {
            holder.dayTextView.setTextColor(Color.WHITE)
            holder.itemView.setBackgroundColor(Color.BLACK)
            holder.underlineView.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        } else {
            holder.dayTextView.setTextColor(Color.BLACK)
            holder.itemView.setBackgroundColor(Color.WHITE)
            holder.underlineView.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        }

        if (day.isNotEmpty()) {
            val hasUnderline = dateRangesWithSchedules.contains(dateKey)
            val hasDot = datesWithSchedules.contains(dateKey)
            val hasEmoji = emojiMap.containsKey(dateKey)

            holder.underlineView.visibility = if (hasUnderline) View.VISIBLE else View.GONE
            holder.emojiView.visibility = if (hasEmoji) View.VISIBLE else View.GONE
            holder.dotView.visibility = if (hasDot) View.VISIBLE else View.GONE

            if (hasEmoji) {
                holder.emojiView.setImageResource(emojiMap[dateKey]!!)
            }
        } else {
            holder.dotView.visibility = View.GONE
            holder.underlineView.visibility = View.GONE
            holder.emojiView.visibility = View.GONE
        }

        if (emojiMap.containsKey(dateKey)) {
            holder.emojiView.setImageResource(emojiMap[dateKey]!!)
            holder.emojiView.visibility = View.VISIBLE
        } else {
            holder.emojiView.visibility = View.GONE
        }

        if ((position + 1) % 7 == 0) {
            holder.dayTextView.setTextColor(Color.BLUE) //토요일
        } else if (position % 7 == 0) {
            holder.dayTextView.setTextColor(Color.RED) //일요일
        }

        holder.itemView.setOnClickListener {
            if (day.isNotEmpty()) {
                activity?.let {
                    val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
                    val dialogTheme = if (isDarkMode) {
                        R.style.DarkDialogTheme
                    } else {
                        R.style.LightDialogTheme
                    }

                    val selectedDay = day.toInt()
                    val selectedDate =
                        "${calendar.get(Calendar.YEAR)}.${calendar.get(Calendar.MONTH) + 1}.$selectedDay"

                    val dialogView = LayoutInflater.from(it).inflate(R.layout.dialog_event, null)
                    val dialogDate = dialogView.findViewById<TextView>(R.id.dialog_date)
                    val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
                    val editTitle = dialogView.findViewById<EditText>(R.id.edit_schedule)
                    dialogDate.text = selectedDate

                    if (isDarkMode) {
                        dialogView.setBackgroundColor(Color.BLACK)
                        dialogTitle.setTextColor(Color.WHITE)
                        dialogDate.setTextColor(Color.WHITE)
                        editTitle.setTextColor(Color.WHITE)
                        editTitle.setHintTextColor(Color.GRAY)
                        editTitle.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                    } else {
                        dialogView.setBackgroundColor(Color.WHITE)
                        dialogTitle.setTextColor(Color.BLACK)
                        dialogDate.setTextColor(Color.BLACK)
                        editTitle.setTextColor(Color.BLACK)
                        editTitle.setHintTextColor(Color.GRAY)
                        editTitle.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
                    }

                    if (emojiMap.containsKey(selectedDate)) {
                        val emojiResId = emojiMap[selectedDate]
                        if (emojiResId == R.drawable.baseline_star_24) {
                            editTitle.append("⭐")
                        } else if (emojiResId == R.drawable.baseline_favorite_24) {
                            editTitle.append("❤️")
                        } else if (emojiResId == R.drawable.baseline_cake_24) {
                            editTitle.append("🎂")
                        }
                    }

                    // 별 이모지 버튼
                    dialogView.findViewById<ImageView>(R.id.emoji_1).setOnClickListener {
                        editTitle.append("⭐")
                    }

                    // 하트 이모지 버튼
                    dialogView.findViewById<ImageView>(R.id.emoji_2).setOnClickListener {
                        editTitle.append("❤️")
                    }

                    // 케이크 이모지 버튼
                    dialogView.findViewById<ImageView>(R.id.emoji_3).setOnClickListener {
                        editTitle.append("🎂")
                    }

                    val builder = AlertDialog.Builder(it, dialogTheme)
                    builder.setView(dialogView)
                    builder.setPositiveButton("일정 추가") { dialog, _ ->
                        onDateSelected(selectedDate)
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
                    builder.setNeutralButton("확인") { dialog, _ ->
                        val emojiText = editTitle.text.toString()
                        when {
                            emojiText.contains("⭐") -> emojiMap[selectedDate] = R.drawable.baseline_star_24
                            emojiText.contains("❤️") -> emojiMap[selectedDate] = R.drawable.baseline_favorite_24
                            emojiText.contains("🎂") -> emojiMap[selectedDate] = R.drawable.baseline_cake_24
                            else -> emojiMap.remove(selectedDate)
                        }
                        notifyDataSetChanged()
                        dialog.dismiss()
                    }
                    builder.show()
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            if (day.isNotEmpty()) {
                activity?.let {
                    val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
                    val dialogTheme = if (isDarkMode) {
                        R.style.DarkDialogTheme
                    } else {
                        R.style.LightDialogTheme
                    }

                    val selectedDay = day.toInt()
                    val selectedDate = "${calendar.get(Calendar.YEAR)}.${calendar.get(Calendar.MONTH) + 1}.$selectedDay"

                    val sharedPreferences = it.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)

                    var isInRange = false
                    var isFirstDateInRange = false
                    var startDate = selectedDate
                    var endDate: String? = null

                    for (dateKey in dateRangesWithSchedules) {
                        val rangeEndDate = sharedPreferences.getString("${dateKey}_range", "")
                        if (!rangeEndDate.isNullOrEmpty()) {
                            val startParts = dateKey.split(".").map { it.toInt() }
                            val endParts = rangeEndDate.split(".").map { it.toInt() }

                            val startCal = Calendar.getInstance().apply {
                                set(startParts[0], startParts[1] - 1, startParts[2])
                            }
                            val endCal = Calendar.getInstance().apply {
                                set(endParts[0], endParts[1] - 1, endParts[2])
                            }

                            val targetCal = Calendar.getInstance().apply {
                                set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selectedDay)
                            }

                            // 선택한 날짜가 범위에 포함되는지 확인
                            if (!targetCal.before(startCal) && !targetCal.after(endCal)) {
                                isInRange = true
                                startDate = dateKey
                                endDate = rangeEndDate
                                if (selectedDate == dateKey) {
                                    isFirstDateInRange = true
                                }
                                break
                            }
                        }
                    }

                    val builder = AlertDialog.Builder(it, dialogTheme)

                    if (!isInRange || isFirstDateInRange) {
                        val dialogView = LayoutInflater.from(it).inflate(R.layout.dialog_period, null)
                        val dateRangeEditText = dialogView.findViewById<EditText>(R.id.date_range_edit_text)
                        val calendarIcon = dialogView.findViewById<ImageView>(R.id.calendar_icon)
                        val dialogDate = dialogView.findViewById<TextView>(R.id.dialog_date)
                        val calendarText = dialogView.findViewById<EditText>(R.id.calendar_text)
                        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)

                        dialogDate.text = startDate
                        dateRangeEditText.setText(endDate)
                        calendarText.setText(sharedPreferences.getString("${startDate}_text", ""))

                        if (isDarkMode) {
                            dialogView.setBackgroundColor(Color.BLACK)
                            dialogDate.setTextColor(Color.WHITE)
                            dialogTitle.setTextColor(Color.GRAY)
                            dateRangeEditText.setTextColor(Color.WHITE)
                            dateRangeEditText.setHintTextColor(Color.GRAY)
                            dateRangeEditText.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                            calendarText.setTextColor(Color.WHITE)
                            calendarText.setHintTextColor(Color.GRAY)
                            calendarText.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                            calendarIcon.setColorFilter(Color.WHITE)
                        } else {
                            dialogView.setBackgroundColor(Color.WHITE)
                            dialogDate.setTextColor(Color.BLACK)
                            dialogTitle.setTextColor(Color.GRAY)
                            dateRangeEditText.setTextColor(Color.BLACK)
                            dateRangeEditText.setHintTextColor(Color.DKGRAY)
                            dateRangeEditText.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
                            calendarText.setTextColor(Color.BLACK)
                            calendarText.setHintTextColor(Color.DKGRAY)
                            calendarText.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
                            calendarIcon.setColorFilter(Color.BLACK)
                        }

                        builder.setView(dialogView)

                        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                            val endDateCal = Calendar.getInstance()
                            endDateCal.set(year, month, dayOfMonth)
                            val endDateString = "${year}.${month + 1}.$dayOfMonth"
                            dateRangeEditText.setText(endDateString)
                        }

                        val endDatePickerDialog = DatePickerDialog(it,
                            if (isDarkMode) R.style.DarkSpinnerDatePickerDialogTheme else R.style.SpinnerDatePickerDialogTheme,
                            datePickerListener,
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        // DatePickerDialog를 보여줄 때 버튼의 스타일을 변경
                        endDatePickerDialog.setOnShowListener {
                            val positiveButton = endDatePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                            val negativeButton = endDatePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

                            if (isDarkMode) {
                                positiveButton.setTextColor(Color.WHITE)
                                negativeButton.setTextColor(Color.WHITE)
                                positiveButton.setBackgroundColor(Color.BLACK)
                                negativeButton.setBackgroundColor(Color.BLACK)

                                // 버튼 바의 배경색 변경
                                val buttonBar = positiveButton.parent as ViewGroup
                                buttonBar.setBackgroundColor(Color.BLACK)
                                // 버튼 컨테이너의 배경색을 검정색으로 설정
                                val parentView = buttonBar.parent as ViewGroup
                                parentView.setBackgroundColor(Color.BLACK)

                            } else {
                                positiveButton.setTextColor(Color.BLACK)
                                negativeButton.setTextColor(Color.BLACK)
                                positiveButton.setBackgroundColor(Color.WHITE)
                                negativeButton.setBackgroundColor(Color.WHITE)

                                // 버튼 바의 배경색 변경
                                val buttonBar = positiveButton.parent as ViewGroup
                                buttonBar.setBackgroundColor(Color.WHITE)
                                // 버튼 컨테이너의 배경색을 검정색으로 설정
                                val parentView = buttonBar.parent as ViewGroup
                                parentView.setBackgroundColor(Color.WHITE)
                            }


                        }

                        dateRangeEditText.setOnClickListener {
                            endDatePickerDialog.show()
                        }

                        calendarIcon.setOnClickListener {
                            endDatePickerDialog.show()
                        }

                        builder.setPositiveButton("저장") { dialog, _ ->
                            val newEndDate = dateRangeEditText.text.toString()

                            val previousEndDate = sharedPreferences.getString("${startDate}_range", "")
                            if (!previousEndDate.isNullOrEmpty()) {
                                val startParts = startDate.split(".").map { it.toInt() }
                                val previousEndParts = previousEndDate.split(".").map { it.toInt() }

                                val startCal = Calendar.getInstance().apply {
                                    set(startParts[0], startParts[1] - 1, startParts[2])
                                }
                                val previousEndCal = Calendar.getInstance().apply {
                                    set(previousEndParts[0], previousEndParts[1] - 1, previousEndParts[2])
                                }

                                val editor = sharedPreferences.edit()
                                while (!startCal.after(previousEndCal)) {
                                    val dateKey = "${startCal.get(Calendar.YEAR)}.${startCal.get(Calendar.MONTH) + 1}.${startCal.get(Calendar.DAY_OF_MONTH)}"
                                    dateRangesWithSchedules.remove(dateKey)
                                    editor.remove("${dateKey}_range")
                                    editor.remove("${dateKey}_text")
                                    startCal.add(Calendar.DAY_OF_MONTH, 1)
                                }
                                editor.apply()
                            }

                            if (calendarText.text.toString().isEmpty()) {
                            } else {
                                onDateRangeSelected(startDate, newEndDate)
                                dateRangesWithSchedules.add(startDate)

                                with(sharedPreferences.edit()) {
                                    putString("${startDate}_range", newEndDate)
                                    putString("${startDate}_text", calendarText.text.toString())
                                    apply()
                                }
                            }

                            notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    } else {
                        val dialogView = LayoutInflater.from(it).inflate(R.layout.dialog_periodset, null)
                        val dateRangeTitle = dialogView.findViewById<TextView>(R.id.date_range_title)
                        val dateRangeMessage = dialogView.findViewById<TextView>(R.id.date_range_message)

                        val dialogTitle = "$startDate - $endDate"
                        dateRangeTitle.text = dialogTitle

                        val calendarText = sharedPreferences.getString("${startDate}_text", "")
                        dateRangeMessage.text = calendarText

                        if (isDarkMode) {
                            dialogView.setBackgroundColor(Color.BLACK)
                            dateRangeTitle.setTextColor(Color.WHITE)
                            dateRangeMessage.setTextColor(Color.WHITE)
                        } else {
                            dialogView.setBackgroundColor(Color.WHITE)
                            dateRangeTitle.setTextColor(Color.BLACK)
                            dateRangeMessage.setTextColor(Color.BLACK)
                        }

                        builder.setView(dialogView)
                        builder.setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
                    }

                    builder.show()
                }
            }
            true
        }

    }

    override fun getItemCount(): Int {
        return daysOfMonth.size
    }

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.day_text)
        val dotView: View = itemView.findViewById(R.id.dot_view)
        val underlineView: View = itemView.findViewById(R.id.underline_view)
        val emojiView: ImageView = itemView.findViewById(R.id.emoji_image)
    }
}
