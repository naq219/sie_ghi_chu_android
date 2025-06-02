package quangan.sreminder.utils

import java.util.*

/**
 * Utility class để xử lý chuyển đổi giữa lịch dương và lịch âm
 * Sử dụng thuật toán chuyển đổi lịch âm Việt Nam
 */
object LunarCalendarUtils {
    
    // Bảng số ngày trong tháng âm lịch (không nhuận và nhuận)
    private val LUNAR_MONTH_DAYS = intArrayOf(29, 30)
    
    // Ngày cơ sở để tính toán (1/1/1900 dương lịch = 11/12/1899 âm lịch)
    private const val BASE_YEAR = 1900
    private const val BASE_MONTH = 1
    private const val BASE_DAY = 31
    
    data class LunarDate(
        val day: Int,
        val month: Int,
        val year: Int,
        val isLeapMonth: Boolean = false
    )
    
    /**
     * Chuyển đổi từ dương lịch sang âm lịch
     */
    fun solarToLunar(solarDate: Date): LunarDate {
        val calendar = Calendar.getInstance().apply { time = solarDate }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Thuật toán chuyển đổi đơn giản (có thể cần cải thiện độ chính xác)
        val totalDays = getTotalDaysFromBase(year, month, day)
        return calculateLunarDate(totalDays)
    }
    
    /**
     * Chuyển đổi từ âm lịch sang dương lịch
     */
    fun lunarToSolar(lunarDate: LunarDate): Date {
        val totalDays = getTotalLunarDays(lunarDate)
        return calculateSolarDate(totalDays)
    }
    
    /**
     * Lấy ngày âm lịch tháng tiếp theo
     */
    fun getNextLunarMonth(currentDate: Date): Date {
        val lunarDate = solarToLunar(currentDate)
        val nextLunarDate = if (lunarDate.month == 12) {
            lunarDate.copy(month = 1, year = lunarDate.year + 1)
        } else {
            lunarDate.copy(month = lunarDate.month + 1)
        }
        return lunarToSolar(nextLunarDate)
    }
    
    /**
     * Lấy ngày âm lịch năm tiếp theo
     */
    fun getNextLunarYear(currentDate: Date): Date {
        val lunarDate = solarToLunar(currentDate)
        val nextLunarDate = lunarDate.copy(year = lunarDate.year + 1)
        return lunarToSolar(nextLunarDate)
    }
    
    /**
     * Kiểm tra năm âm lịch có nhuận không
     */
    fun isLunarLeapYear(year: Int): Boolean {
        // Thuật toán đơn giản: năm nhuận âm lịch xảy ra khoảng 7 lần trong 19 năm
        return (year * 12 + 7) % 19 < 12
    }
    
    /**
     * Lấy tháng nhuận trong năm âm lịch (nếu có)
     */
    fun getLeapMonth(year: Int): Int? {
        if (!isLunarLeapYear(year)) return null
        // Thuật toán đơn giản để xác định tháng nhuận
        return ((year * 12 + 7) % 19) % 12 + 1
    }
    
    private fun getTotalDaysFromBase(year: Int, month: Int, day: Int): Int {
        val baseCalendar = Calendar.getInstance().apply {
            set(BASE_YEAR, BASE_MONTH - 1, BASE_DAY)
        }
        val targetCalendar = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        
        val diffInMillis = targetCalendar.timeInMillis - baseCalendar.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
    
    private fun calculateLunarDate(totalDays: Int): LunarDate {
        // Thuật toán đơn giản để tính ngày âm lịch
        var remainingDays = totalDays
        var year = BASE_YEAR - 1
        var month = 12
        var day = 11
        
        // Tính năm
        while (remainingDays > 0) {
            val daysInYear = if (isLunarLeapYear(year)) 384 else 354
            if (remainingDays >= daysInYear) {
                remainingDays -= daysInYear
                year++
            } else {
                break
            }
        }
        
        // Tính tháng
        month = 1
        while (remainingDays > 0) {
            val daysInMonth = getLunarMonthDays(year, month)
            if (remainingDays >= daysInMonth) {
                remainingDays -= daysInMonth
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            } else {
                break
            }
        }
        
        day = remainingDays + 1
        
        return LunarDate(day, month, year)
    }
    
    private fun getTotalLunarDays(lunarDate: LunarDate): Int {
        var totalDays = 0
        
        // Tính tổng số ngày từ năm cơ sở đến năm hiện tại
        for (year in (BASE_YEAR - 1) until lunarDate.year) {
            totalDays += if (isLunarLeapYear(year)) 384 else 354
        }
        
        // Tính tổng số ngày từ tháng 1 đến tháng hiện tại
        for (month in 1 until lunarDate.month) {
            totalDays += getLunarMonthDays(lunarDate.year, month)
        }
        
        // Cộng số ngày trong tháng hiện tại
        totalDays += lunarDate.day - 1
        
        return totalDays
    }
    
    private fun calculateSolarDate(totalDays: Int): Date {
        val baseCalendar = Calendar.getInstance().apply {
            set(BASE_YEAR, BASE_MONTH - 1, BASE_DAY)
        }
        
        baseCalendar.add(Calendar.DAY_OF_MONTH, totalDays)
        return baseCalendar.time
    }
    
    private fun getLunarMonthDays(year: Int, month: Int): Int {
        // Thuật toán đơn giản: tháng lẻ 30 ngày, tháng chẵn 29 ngày
        // Trong thực tế cần tra bảng lịch âm chính xác
        return if (month % 2 == 1) 30 else 29
    }
    
    /**
     * Format ngày âm lịch thành chuỗi
     */
    fun formatLunarDate(lunarDate: LunarDate): String {
        val leapText = if (lunarDate.isLeapMonth) " (nhuận)" else ""
        return "${lunarDate.day}/${lunarDate.month}/${lunarDate.year}$leapText (ÂL)"
    }
    
    /**
     * Format ngày dương lịch kèm ngày âm lịch
     */
    fun formatDateWithLunar(solarDate: Date): String {
        val lunarDate = solarToLunar(solarDate)
        val solarFormat = android.text.format.DateFormat.format("dd/MM/yyyy", solarDate)
        val lunarFormat = formatLunarDate(lunarDate)
        return "$solarFormat - $lunarFormat"
    }
}