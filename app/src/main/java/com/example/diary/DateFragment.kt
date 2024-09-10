package com.example.diary

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DateFragment : Fragment() {

    private lateinit var dateTextView: TextView
    private lateinit var hintTextView: TextView
    private lateinit var addButton: ImageButton
    private lateinit var backButton1: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DateAdapter
    private val schedules = mutableListOf<Schedule>()
    private var date: String? = null

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: String): DateFragment {
            val fragment = DateFragment()
            val args = Bundle()
            args.putString(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_date, container, false)
        dateTextView = view.findViewById(R.id.date_text_view)
        hintTextView = view.findViewById(R.id.hint_text_view)
        recyclerView = view.findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = DateAdapter(schedules)
        recyclerView.adapter = adapter

        addButton = view.findViewById(R.id.add_button)
        backButton1 = view.findViewById(R.id.back_button1)
        deleteButton = view.findViewById(R.id.delete_button)

        date = arguments?.getString(ARG_DATE)
        dateTextView.text = date

        loadData()
        updateHintVisibility()
        updateUIForDarkMode()

        addButton.setOnClickListener {
            val scheduleFragment = ScheduleFragment()
            val args = Bundle()
            args.putString("date", date)
            scheduleFragment.arguments = args
            scheduleFragment.setTargetFragment(this, 1)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, scheduleFragment)
                .addToBackStack(null)
                .commit()
        }

        backButton1.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        deleteButton.setOnClickListener {
            deleteCheckedSchedules()
        }

        parentFragmentManager.setFragmentResultListener("update_request_key", this) { _, bundle ->
            val title = bundle.getString("title")
            val content = bundle.getString("content")
            val time = bundle.getString("time")
            val originalTitle = bundle.getString("original_title")
            val color = bundle.getInt("color", 0x00000000)

            // 기존 일정이 있다면 삭제
            if (!originalTitle.isNullOrEmpty()) {
                val index = schedules.indexOfFirst { it.title == originalTitle }
                if (index != -1) {
                    schedules.removeAt(index) // 기존 일정을 삭제
                }
            }

            // 새 일정을 맨 위에 추가
            schedules.add(0, Schedule(title ?: "", content ?: "", time ?: "", color))

            // 어댑터에게 데이터 변경을 알림
            adapter.notifyDataSetChanged()

            // 데이터 저장
            saveData()

            // 힌트 표시 여부 업데이트
            updateHintVisibility()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        updateUIForDarkMode()
    }

    fun updateUIForDarkMode() {
        val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
        val backgroundColor = if (isDarkMode) Color.BLACK else Color.WHITE
        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK
        val hintTextColor = if (isDarkMode) Color.LTGRAY else Color.DKGRAY

        view?.setBackgroundColor(backgroundColor)

        dateTextView.setTextColor(textColor)
        hintTextView.setTextColor(hintTextColor)
        addButton.imageTintList = ColorStateList.valueOf(textColor)
        backButton1.imageTintList = ColorStateList.valueOf(textColor)
        deleteButton.imageTintList = ColorStateList.valueOf(textColor)

        adapter.notifyDataSetChanged()
    }

    private fun loadData() {
        date?.let {
            val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
            val savedSchedules = sharedPreferences?.getStringSet(it, emptySet())
            savedSchedules?.let { scheduleSet ->
                schedules.clear()

                // 데이터를 리스트에 추가할 때 시간 순서로 추가
                val sortedSchedules = scheduleSet.toList().sortedByDescending { scheduleString ->
                    // 추가된 시간 또는 데이터를 기준으로 정렬
                    scheduleString // 최신 항목이 위로 오도록 정렬
                }

                sortedSchedules.forEach { scheduleString ->
                    val parts = scheduleString.split("|")
                    if (parts.size == 4) {  // 색상 데이터를 포함하여 4개의 요소를 처리
                        val title = parts[0]
                        val content = parts[1]
                        val time = parts[2]
                        val color = parts[3].toIntOrNull() ?: 0x00000000 // 색상 정보 추가
                        schedules.add(Schedule(title, content, time, color))
                    }
                }
                adapter.notifyDataSetChanged()
                updateHintVisibility()
            }
        }
    }

    private fun saveData() {
        date?.let {
            val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
            val editor = sharedPreferences?.edit()
            val scheduleSet = schedules.map { schedule ->
                "${schedule.title}|${schedule.content}|${schedule.time}|${schedule.color}" // 색상 정보를 포함
            }.toSet()
            editor?.putStringSet(it, scheduleSet)
            editor?.apply()
        }
    }

    private fun deleteCheckedSchedules() {
        val checkedTitles = adapter.getCheckedTitles()
        schedules.removeAll { it.title in checkedTitles }
        adapter.notifyDataSetChanged()
        saveData()
        updateHintVisibility()
        updateUIForDarkMode()
    }

    private fun updateHintVisibility() {
        if (schedules.isEmpty()) {
            hintTextView.visibility = View.VISIBLE
        } else {
            hintTextView.visibility = View.GONE
        }
    }

    inner class DateAdapter(private val items: MutableList<Schedule>) : RecyclerView.Adapter<DateAdapter.ViewHolder>() {

        private val checkedItems = mutableSetOf<String>() // 체크된 항목의 제목을 저장

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleCheckBox: CheckBox = itemView.findViewById(R.id.item_title_checkbox)
            val timeTextView: TextView = itemView.findViewById(R.id.item_time_text)
            val editButton: ImageButton = itemView.findViewById(R.id.edit_button)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_title, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val schedule = items[position]
            holder.titleCheckBox.text = schedule.title
            holder.timeTextView.text = schedule.time
            holder.titleCheckBox.isChecked = checkedItems.contains(schedule.title)

            val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
            val textColor = if (isDarkMode) Color.WHITE else Color.BLACK
            val checkboxColor = if (isDarkMode) schedule.color else schedule.color

            // 체크박스와 텍스트 색상 설정
            holder.titleCheckBox.buttonTintList = ColorStateList.valueOf(checkboxColor)
            holder.titleCheckBox.setTextColor(textColor)
            holder.timeTextView.setTextColor(textColor)
            holder.editButton.imageTintList = ColorStateList.valueOf(textColor)

            // 체크박스의 체크 상태에 따른 색상 설정
            holder.titleCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkedItems.add(schedule.title)
                } else {
                    checkedItems.remove(schedule.title)
                }
            }

            holder.editButton.setOnClickListener {
                val scheduleFragment = ScheduleFragment()
                val args = Bundle().apply {
                    putString("title", schedule.title)
                    putString("content", schedule.content)
                    putString("time", schedule.time)  // 시간 정보 전달
                    putString("date", date)
                    putInt("color", schedule.color)
                }
                scheduleFragment.arguments = args

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, scheduleFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        override fun getItemCount(): Int = items.size

        fun getCheckedTitles(): Set<String> {
            return checkedItems
        }
    }
}
