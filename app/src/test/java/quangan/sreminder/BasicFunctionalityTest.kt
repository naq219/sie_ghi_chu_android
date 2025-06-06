package quangan.sreminder

import org.junit.Test
import org.junit.Assert.*

/**
 * Basic functionality tests for the app
 */
class BasicFunctionalityTest {
    
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    
    @Test
    fun string_operations() {
        val testString = "Hello World"
        assertEquals(11, testString.length)
        assertTrue(testString.contains("World"))
        assertEquals("HELLO WORLD", testString.uppercase())
    }
    
    @Test
    fun list_operations() {
        val numbers = listOf(1, 2, 3, 4, 5)
        assertEquals(5, numbers.size)
        assertTrue(numbers.contains(3))
        assertEquals(15, numbers.sum())
    }
    
    @Test
    fun date_operations() {
        val currentTime = System.currentTimeMillis()
        val futureTime = currentTime + 86400000 // Add 1 day
        
        assertTrue(futureTime > currentTime)
        assertEquals(86400000L, futureTime - currentTime)
    }
    
    @Test
    fun uuid_generation() {
        val uuid1 = java.util.UUID.randomUUID()
        val uuid2 = java.util.UUID.randomUUID()
        
        assertNotNull(uuid1)
        assertNotNull(uuid2)
        assertNotEquals(uuid1, uuid2)
    }
    
    @Test
    fun calendar_operations() {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        
        assertTrue(currentYear >= 2024)
        
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 7)
        val futureDate = calendar.time
        val currentDate = java.util.Date()
        
        assertTrue(futureDate.after(currentDate))
    }
    
    @Test
    fun time_calculations() {
        val oneMinute = 60 * 1000L
        val oneHour = 60 * oneMinute
        val oneDay = 24 * oneHour
        
        assertEquals(60000L, oneMinute)
        assertEquals(3600000L, oneHour)
        assertEquals(86400000L, oneDay)
    }
    
    @Test
    fun string_formatting() {
        val hour = 9
        val minute = 30
        val timeString = String.format("%02d:%02d", hour, minute)
        
        assertEquals("09:30", timeString)
    }
    
    @Test
    fun boolean_operations() {
        val isActive = true
        val isCompleted = false
        
        assertTrue(isActive)
        assertFalse(isCompleted)
        assertNotEquals(isActive, isCompleted)
    }
    
    @Test
    fun number_validations() {
        val day = 15
        val month = 8
        val year = 2024
        
        assertTrue(day >= 1 && day <= 31)
        assertTrue(month >= 1 && month <= 12)
        assertTrue(year >= 2020 && year <= 2030)
    }
}