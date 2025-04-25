package com.example.parkingtimerapp.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.NumberPicker
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

class FixedFormatDatePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DatePicker(context, attrs, defStyleAttr) {

    private var yearPicker: NumberPicker? = null
    private var monthPicker: NumberPicker? = null
    private var dayPicker: NumberPicker? = null
    private var onDateChangedListener: OnDateChangedListener? = null

    init {
        try {
            // Get the internal LinearLayout that contains the pickers
            val pickerLayout = getChildAt(0) as ViewGroup

            // Remove all existing pickers
            pickerLayout.removeAllViews()

            // Create and add pickers in our desired order: YEAR-MONTH-DAY
            yearPicker = createYearPicker()
            monthPicker = createMonthPicker()
            dayPicker = createDayPicker()

            // Add pickers in the desired order
            pickerLayout.addView(yearPicker)
            pickerLayout.addView(monthPicker)
            pickerLayout.addView(dayPicker)

            // Set initial date
            val calendar = Calendar.getInstance()
            init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 
                 calendar.get(Calendar.DAY_OF_MONTH), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun init(year: Int, monthOfYear: Int, dayOfMonth: Int, onDateChangedListener: OnDateChangedListener?) {
        super.init(year, monthOfYear, dayOfMonth, onDateChangedListener)
        this.onDateChangedListener = onDateChangedListener
        updatePickers(year, monthOfYear, dayOfMonth)
    }

    private fun updatePickers(year: Int, month: Int, day: Int) {
        yearPicker?.value = year
        monthPicker?.value = month
        dayPicker?.value = day
    }

    private fun createYearPicker(): NumberPicker {
        return NumberPicker(context).apply {
            minValue = 2020
            maxValue = 2030
            value = Calendar.getInstance().get(Calendar.YEAR)
            setOnValueChangedListener { _, _, newVal ->
                Log.d("FixedFormatDatePicker", "Year changed to: $newVal")
                updateDate(newVal, monthPicker?.value ?: 0, dayPicker?.value ?: 1)
            }
        }
    }

    private fun createMonthPicker(): NumberPicker {
        return NumberPicker(context).apply {
            minValue = 0
            maxValue = 11
            
            // Get month names in current locale
            val locale = Locale.getDefault()
            val months = DateFormatSymbols(locale).months.take(12).toTypedArray()
            displayedValues = months
            
            value = Calendar.getInstance().get(Calendar.MONTH)
            setOnValueChangedListener { _, _, newVal ->
                Log.d("FixedFormatDatePicker", "Month changed to: $newVal")
                updateDate(yearPicker?.value ?: 2023, newVal, dayPicker?.value ?: 1)
            }
        }
    }

    private fun createDayPicker(): NumberPicker {
        return NumberPicker(context).apply {
            minValue = 1
            maxValue = 31
            val days = (1..31).map { String.format("%02d", it) }.toTypedArray()
            displayedValues = days
            value = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            setOnValueChangedListener { _, _, newVal ->
                Log.d("FixedFormatDatePicker", "Day changed to: $newVal")
                updateDate(yearPicker?.value ?: 2023, monthPicker?.value ?: 0, newVal)
            }
        }
    }

    override fun updateDate(year: Int, month: Int, dayOfMonth: Int) {
        super.updateDate(year, month, dayOfMonth)
        Log.d("FixedFormatDatePicker", "Updating date to: $year-${month + 1}-$dayOfMonth")
        updatePickers(year, month, dayOfMonth)
    }
} 