package com.example.diary

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class ScheduleFragment : Fragment() {

    private lateinit var backButton2: ImageButton
    private lateinit var saveButton: Button
    private lateinit var editTitle: EditText
    private lateinit var editContent: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var colorPickerButton: Button
    private lateinit var displayColor: View
    private lateinit var imageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var contentTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var colorTextView: TextView

    private var selectedColor: Int = Color.GRAY // 초기색상은 회색
    private var datesToAdd: List<String>? = null
    private var date: String? = null
    private var originalTitle: String? = null
    private var originalContent: String? = null
    private var originalTime: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false

        // 테마를 동적으로 설정한 상태에서 TimePicker를 인플레이트합니다.
        val contextThemeWrapper = ContextThemeWrapper(requireContext(), if (isDarkMode) R.style.DarkTimePickerStyle else R.style.LightTimePickerStyle)
        val view = inflater.cloneInContext(contextThemeWrapper).inflate(R.layout.fragment_schedule, container, false)

        saveButton = view.findViewById(R.id.save_button)
        editTitle = view.findViewById(R.id.edit_title)
        editContent = view.findViewById(R.id.edit_content)
        backButton2 = view.findViewById(R.id.back_button2)
        timePicker = view.findViewById(R.id.timePicker)
        colorPickerButton = view.findViewById(R.id.colorPicker_button)
        displayColor = view.findViewById(R.id.displayColor)
        imageView = view.findViewById(R.id.imageView)
        titleTextView = view.findViewById(R.id.titleTextView)
        contentTextView = view.findViewById(R.id.contentTextView)
        timeTextView = view.findViewById(R.id.timeTextView)
        colorTextView = view.findViewById(R.id.colorTextView)

        timePicker.setIs24HourView(false) // 24시간 형식을 설정합니다.

        val args = arguments
        if (args != null) {
            datesToAdd = args.getStringArrayList("datesToAdd")
            date = args.getString("date")
            originalTitle = args.getString("title")
            originalContent = args.getString("content")
            originalTime = args.getString("time")

            selectedColor = args.getInt("color", Color.GRAY) // 색상 정보 가져오기

        }

        originalTitle?.let { editTitle.setText(it) }
        originalContent?.let { editContent.setText(it) }
        originalTime?.let {
            val parts = it.split(" ")
            val amPm = parts[0]
            val timeParts = parts[1].split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            timePicker.hour = if (amPm == "AM") hour else hour + 12
            timePicker.minute = minute
        }

        // 선택된 색상을 표시
        displayColor.backgroundTintList = ColorStateList.valueOf(selectedColor)

        colorPickerButton.setOnClickListener {
            showColorPickerDialog()
        }

        saveButton.setOnClickListener {
            val title = editTitle.text.toString()
            val content = editContent.text.toString()
            val hour = timePicker.hour
            val minute = timePicker.minute
            val amPm = if (hour < 12) "AM" else "PM"
            val formattedHour = if (hour == 0 || hour == 12) 12 else hour % 12

            val time = String.format("%s %02d:%02d", amPm, formattedHour, minute)

            val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
            val editor = sharedPreferences?.edit()

            datesToAdd?.forEach { dateKey ->
                val scheduleSet = sharedPreferences?.getStringSet(dateKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                if (!originalTitle.isNullOrEmpty()) {
                    scheduleSet.removeIf { it.startsWith("$originalTitle|") }
                }
                scheduleSet.add("$title|$content|$time|$selectedColor")
                editor?.putStringSet(dateKey, scheduleSet)
            }
            date?.let {
                val scheduleSet = sharedPreferences?.getStringSet(it, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                if (!originalTitle.isNullOrEmpty()) {
                    scheduleSet.removeIf { it.startsWith("$originalTitle|") }
                }
                scheduleSet.add("$title|$content|$time|$selectedColor")
                editor?.putStringSet(it, scheduleSet)

            }
            editor?.apply()

            parentFragmentManager.popBackStack()
        }

        backButton2.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        updateUIForDarkMode()

        return view
    }

    override fun onResume() {
        super.onResume()
        updateUIForDarkMode()
    }

    private fun showColorPickerDialog() {
        val colors = resources.getIntArray(R.array.colors)

        val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
        val builder = AlertDialog.Builder(
            ContextThemeWrapper(
                requireContext(),
                if (isDarkMode) R.style.DarkDialogTheme else R.style.LightDialogTheme
            )
        )
        builder.setTitle("색상을 선택하세요")

        // GridLayout을 사용하여 색상 뷰를 5x2로 정렬
        val colorOptions = GridLayout(context)
        colorOptions.rowCount = 2
        colorOptions.columnCount = 5
        colorOptions.setPadding(16, 16, 16, 16)

        colors.forEach { color ->
            val colorView = View(context)
            val params = GridLayout.LayoutParams()
            params.width = 120
            params.height = 120
            params.setMargins(30, 20, 20, 20)
            colorView.layoutParams = params

            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(color)
            colorView.background = drawable

            colorView.setOnClickListener {
                selectedColor = color
                displayColor.backgroundTintList = ColorStateList.valueOf(color)
                builder.create().dismiss()
            }
            colorOptions.addView(colorView)
        }

        builder.setView(colorOptions)
        builder.setNegativeButton("확인") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun updateUIForDarkMode() {
        val isDarkMode = (activity as? MainActivity)?.getDarkModeState() ?: false
        val backgroundColor = if (isDarkMode) Color.BLACK else Color.WHITE
        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK
        val hintTextColor = if (isDarkMode) Color.GRAY else Color.GRAY
        val displayColorBackground = if (isDarkMode) selectedColor else selectedColor


        view?.setBackgroundColor(backgroundColor)

        editTitle.setTextColor(textColor)
        editTitle.setHintTextColor(hintTextColor)
        editContent.setTextColor(textColor)
        editContent.setHintTextColor(hintTextColor)
        saveButton.setTextColor(textColor)
        backButton2.imageTintList = ColorStateList.valueOf(textColor)
        colorPickerButton.setTextColor(textColor)
        displayColor.backgroundTintList = ColorStateList.valueOf(displayColorBackground)
        imageView.imageTintList = ColorStateList.valueOf(textColor)

        // TextView들 색상 변경
        titleTextView.setTextColor(textColor)
        contentTextView.setTextColor(textColor)
        timeTextView.setTextColor(textColor)
        colorTextView.setTextColor(textColor)

        // EditText 색상 변경
        editTitle.setTextColor(textColor)
        editTitle.setHintTextColor(hintTextColor)
        editContent.setTextColor(textColor)
        editContent.setHintTextColor(hintTextColor)

        // 버튼 및 기타 UI 요소 색상 변경
        saveButton.setTextColor(textColor)
        backButton2.imageTintList = ColorStateList.valueOf(textColor)
        colorPickerButton.setTextColor(textColor)
        displayColor.backgroundTintList = ColorStateList.valueOf(displayColorBackground)
        imageView.imageTintList = ColorStateList.valueOf(textColor)

        // EditText의 밑줄과 테두리를 흰색으로 변경
        setEditTextUnderlineColor(editTitle, textColor)
        setEditTextUnderlineColor(editContent, textColor)
    }

    private fun setEditTextUnderlineColor(editText: EditText, color: Int) {
        editText.background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}


data class Schedule(val title: String, val content: String, val time: String, val color: Int)

