package com.example.diary

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var monthYearText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var modeButton: ImageButton
    private val calendar: Calendar = Calendar.getInstance()
    private val emojiMap: MutableMap<String, Int> = mutableMapOf()
    private val datesWithSchedules = mutableSetOf<String>() // 일정이 있는 날짜들을 저장하는 집합
    private val dateRangesWithSchedules = mutableSetOf<String>() // 일정 범위가 설정된 날짜들을 저장하는 집합

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        monthYearText = view.findViewById(R.id.month_year_text)
        recyclerView = view.findViewById(R.id.recyclerView)

        val prevButton: ImageButton = view.findViewById(R.id.prev_button)
        val nextButton: ImageButton = view.findViewById(R.id.next_button)
        val helpButton: ImageButton = view.findViewById(R.id.help_button)

        modeButton = view.findViewById(R.id.mode_button)

        // RecyclerView를 7열로 설정하여 달력 형태로 표시
        recyclerView.layoutManager = GridLayoutManager(context, 7)

        prevButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        nextButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        modeButton.setOnClickListener {
            (activity as? MainActivity)?.toggleDarkMode()
            updateUIForDarkMode()
        }

        recyclerView.addItemDecoration(BottomLineDecoration(requireContext())) // 밑줄 추가

        // help_button 클릭 리스너 추가
        helpButton.setOnClickListener {
            showHelpDialog()
        }

        // 저장된 상태가 있으면 달력 상태를 복원
        if (savedInstanceState != null) {
            calendar.timeInMillis = savedInstanceState.getLong("calendar_time")
        }

        loadEmojiMap()
        loadDateRangesWithSchedules()
        updateCalendar()
        updateUIForDarkMode()

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 달력 상태 저장 (시간 정보를 저장)
        outState.putLong("calendar_time", calendar.timeInMillis)
    }

    //다크모드
    fun updateUIForDarkMode() {
        val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
        val rootLayout = view?.findViewById<View>(R.id.fragment_calendar_root)

        if (isDarkMode) {
            rootLayout?.setBackgroundColor(Color.BLACK)
            updateTextColor(rootLayout, Color.WHITE)
            modeButton.setImageResource(R.drawable.ic_dark_mode)
            updateButtonColors(Color.WHITE)
        } else {
            rootLayout?.setBackgroundColor(Color.WHITE)
            updateTextColor(rootLayout, Color.BLACK)
            modeButton.setImageResource(R.drawable.ic_light_mode)
            updateButtonColors(Color.BLACK)
        }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateTextColor(view: View?, color: Int) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextColor(view.getChildAt(i), color)
            }
        } else if (view is TextView) {
            view.setTextColor(color)
        }
    }

    private fun updateButtonColors(color: Int) {
        val prevButton: ImageButton? = view?.findViewById(R.id.prev_button)
        val nextButton: ImageButton? = view?.findViewById(R.id.next_button)
        val helpButton: ImageButton? = view?.findViewById(R.id.help_button)
        val modeButton: ImageButton? = view?.findViewById(R.id.mode_button)

        prevButton?.setColorFilter(color)
        nextButton?.setColorFilter(color)
        helpButton?.setColorFilter(color)
        modeButton?.setColorFilter(color)
    }


    private fun showHelpDialog() {
        activity?.let {
            val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
            val dialogTheme = if (isDarkMode) {
                R.style.DarkDialogTheme
            } else {
                R.style.LightDialogTheme
            }


            val builder = AlertDialog.Builder(it, dialogTheme)
            builder.setTitle("📅 캘린더 사용 설명서")
            builder.setMessage(
                """
                    
            ✨ 날짜를 탭하세요
            - 원하는 날짜를 탭하여 메모를 추가하거나 수정할 수 있습니다.

            📏 날짜를 길게 누르세요
            - 특정 날짜를 길게 누르면 일정 범위를 설정할 수 있습니다.

            🌟 이모지를 추가하세요
            - 날짜를 탭하고 이모지를 선택하여 특별한 날을 표시하세요.

            ↔️ 이전/다음 달로 이동
            - 화살표 버튼을 사용하여 이전 또는 다음 달로 이동하세요.

            🔍 날짜 표시
            - 날짜는 다음과 같이 표시됩니다:
              • 점 (▫️): 일정이 있는 날짜
              • 밑줄 (〰️): 일정 범위에 포함된 날짜
              • 이모지 (🌟): 이모지가 추가된 특별한 날짜
            """.trimIndent()
            )

            builder.setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }

            // 다이얼로그 생성 후 바로 보여줌
            builder.create().show()
        }
    }

    private fun updateCalendar() {
        val sdf = SimpleDateFormat("yyyy.MM", Locale.getDefault())
        monthYearText.text = sdf.format(calendar.time)

        val daysOfMonth = generateCalendarDays()
        loadDatesWithSchedules()

        // 어댑터를 설정하고 달력에 표시할 데이터 제공
        activity?.let {
            recyclerView.adapter = CalendarAdapter(
                daysOfMonth,
                it,
                calendar.clone() as Calendar,
                datesWithSchedules,
                dateRangesWithSchedules,
                emojiMap,
                ::onDateSelected,
                ::onDateRangeSelected,
                ::saveDateRangesWithSchedules
            )
        }
    }

    // DateFragment로 전환
    private fun onDateSelected(date: String) {
        val dateDetailFragment = DateFragment.newInstance(date)
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, dateDetailFragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    private fun onDateRangeSelected(startDate: String, endDate: String) {
        val startParts = startDate.split(".").map { it.toInt() }
        val endParts = endDate.split(".").map { it.toInt() }

        val startCal = Calendar.getInstance().apply {
            set(startParts[0], startParts[1] - 1, startParts[2])
        }
        val endCal = Calendar.getInstance().apply {
            set(endParts[0], endParts[1] - 1, endParts[2])
        }

        val datesToAdd = mutableListOf<String>()
        while (!startCal.after(endCal)) {
            val dateKey = "${startCal.get(Calendar.YEAR)}.${startCal.get(Calendar.MONTH) + 1}.${startCal.get(Calendar.DAY_OF_MONTH)}"
            datesToAdd.add(dateKey)
            dateRangesWithSchedules.add(dateKey)
            startCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        saveDateRangesWithSchedules()

        updateCalendar()
    }

    fun onDateRangeDeleted(dateRange: List<String>) {
        activity?.let {
            val sharedPreferences = it.getPreferences(Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            dateRange.forEach { dateKey ->
                editor.remove(dateKey)
                dateRangesWithSchedules.remove(dateKey)
            }

            editor.apply()
            saveDateRangesWithSchedules()
            updateCalendar()
        }
    }

    private fun loadDatesWithSchedules() {
        val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
        val allSchedules = mutableSetOf<String>()
        sharedPreferences?.all?.forEach { entry ->
            if (entry.value is Set<*>) {
                (entry.value as Set<*>).forEach { _ ->
                    val date = entry.key
                    allSchedules.add(date)
                }
            }
        }
        datesWithSchedules.clear()
        datesWithSchedules.addAll(allSchedules)
    }

    private fun loadDateRangesWithSchedules() {
        val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
        val savedDateRanges = sharedPreferences?.getStringSet("dateRangesWithSchedules", emptySet())
        savedDateRanges?.let { ranges ->
            dateRangesWithSchedules.clear()
            dateRangesWithSchedules.addAll(ranges)
        }
    }

    private fun saveDateRangesWithSchedules() {
        val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        editor?.putStringSet("dateRangesWithSchedules", dateRangesWithSchedules)
        editor?.apply()
    }

    private fun generateCalendarDays(): List<String> {
        val days = mutableListOf<String>()
        val tempCalendar = calendar.clone() as Calendar

        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
        val maxDay = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 첫 주의 공백 처리
        for (i in 1 until firstDayOfWeek) {
            days.add("")
        }

        // 월의 일자를 추가
        for (day in 1..maxDay) {
            days.add(day.toString())
        }

        return days
    }

    private fun loadEmojiMap() {
        val sharedPreferences = activity?.getSharedPreferences("EmojiPrefs", Context.MODE_PRIVATE) ?: return
        val emojiEntries = sharedPreferences.getStringSet("emojiMap", null) ?: return

        emojiEntries.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val date = parts[0]
                val resId = parts[1].toIntOrNull()
                if (resId != null) {
                    emojiMap[date] = resId
                }
            }
        }
    }

    private fun saveEmojiMap() {
        val sharedPreferences = activity?.getSharedPreferences("EmojiPrefs", Context.MODE_PRIVATE) ?: return
        val editor = sharedPreferences.edit()

        val emojiEntries = emojiMap.entries.map { "${it.key}:${it.value}" }.toSet()
        editor.putStringSet("emojiMap", emojiEntries)
        editor.apply()
    }

    override fun onPause() {
        super.onPause()
        saveEmojiMap()
        saveDateRangesWithSchedules()
    }

    override fun onResume() {
        super.onResume()
        loadEmojiMap()
        loadDateRangesWithSchedules()
        loadDatesWithSchedules()
        updateCalendar()
        updateUIForDarkMode()
    }

    class BottomLineDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val paint = Paint().apply {
            color = "#c8c8c8".toColorInt()
            strokeWidth = 3f // 밑줄 굵기를 일관되게 설정 (여기서 줄 굵기를 조절할 수 있습니다)
            style = Paint.Style.STROKE // 점선이 아닌 경우 기본 선 스타일 적용
        }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight

            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as RecyclerView.LayoutParams

                // 첫 주의 날짜 위에 선을 그립니다.
                if (i < 7) {
                    val top = child.top + params.topMargin
                    c.drawLine(left.toFloat(), top.toFloat(), right.toFloat(), top.toFloat(), paint)
                }

                // 각 날짜 밑줄
                val bottom = child.bottom + params.bottomMargin
                c.drawLine(left.toFloat(), bottom.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.set(0, 0, 0, 2) // 하단 여백 조정
        }
    }

}