package com.calendar.tbt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for LunarCalendar
 */
public class LunarCalendarTest {
    
    @Test
    public void testCurrentDate() {
        // Test case: Hôm nay là ngày 1/8/2024 (dương lịch)
        // Kết quả mong đợi: ngày 8/6 âm lịch
        LunarCalendar.LunarDate lunarDate = LunarCalendar.getLunarDate(1, 8, 2025);
        
        System.out.println("Test Current Date:");
        System.out.println("Solar: 1/8/2025");
        System.out.println("Expected Lunar: 8/6");
        System.out.println("Actual Lunar: " + lunarDate.toString());
        
        assertEquals("Lunar day should be 8", 8, lunarDate.day);
        assertEquals("Lunar month should be 6", 6, lunarDate.month);
        assertEquals("Lunar year should be 2024", 2025, lunarDate.year);
        // assertFalse("Should not be leap month", lunarDate.isLeap);
    }
    
    @Test
    public void testKnownDates() {
        // Test một số ngày đã biết chính xác
        System.out.println("\nTest Known Dates:");
        
        // Test ngày 1/1/2024 (Tết Nguyên Đán)
        LunarCalendar.LunarDate tet2024 = LunarCalendar.getLunarDate(10, 2, 2024);
        System.out.println("Solar: 10/2/2024 (Tết) -> Lunar: " + tet2024.toString());
        assertEquals("Tết 2024 should be day 1", 1, tet2024.day);
        assertEquals("Tết 2024 should be month 1", 1, tet2024.month);
        
        // Test ngày 15/8/2024 (Tết Trung Thu)
        LunarCalendar.LunarDate trungThu2024 = LunarCalendar.getLunarDate(17, 9, 2024);
        System.out.println("Solar: 17/9/2024 (Trung Thu) -> Lunar: " + trungThu2024.toString());
        assertEquals("Trung Thu should be day 15", 15, trungThu2024.day);
        assertEquals("Trung Thu should be month 8", 8, trungThu2024.month);
    }
    
    @Test
    public void testLeapMonth() {
        // Test năm nhuận (nếu có)
        System.out.println("\nTest Leap Month:");
        
        // Test một số ngày trong năm 2024 để xem có tháng nhuận không
        for (int day = 1; day <= 5; day++) {
            LunarCalendar.LunarDate lunarDate = LunarCalendar.getLunarDate(day, 8, 2024);
            System.out.println("Solar: " + day + "/8/2024 -> Lunar: " + lunarDate.toString());
        }
    }
    
    @Test
    public void testEdgeCases() {
        System.out.println("\nTest Edge Cases:");
        
        // Test đầu năm
        LunarCalendar.LunarDate startOfYear = LunarCalendar.getLunarDate(1, 1, 2024);
        System.out.println("Solar: 1/1/2024 -> Lunar: " + startOfYear.toString());
        
        // Test cuối năm
        LunarCalendar.LunarDate endOfYear = LunarCalendar.getLunarDate(31, 12, 2024);
        System.out.println("Solar: 31/12/2024 -> Lunar: " + endOfYear.toString());
        
        // Test ngày 30/2 (không tồn tại nhưng test exception)
        try {
            LunarCalendar.getLunarDate(30, 2, 2024);
            fail("Should throw exception for invalid date");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly caught invalid date: " + e.getMessage());
        }
    }
    
    @Test
    public void testCurrentLunarDate() {
        System.out.println("\nTest Current Lunar Date:");
        LunarCalendar.LunarDate current = LunarCalendar.getCurrentLunarDate();
        System.out.println("Current Lunar Date: " + current.toString());
        
        // Kiểm tra các giá trị hợp lệ
        assertTrue("Lunar day should be between 1 and 30", current.day >= 1 && current.day <= 30);
        assertTrue("Lunar month should be between 1 and 12", current.month >= 1 && current.month <= 12);
        assertTrue("Lunar year should be reasonable", current.year >= 1900 && current.year <= 2100);
    }
    
    @Test
    public void testDateRange() {
        System.out.println("\nTest Date Range (August 2024):");
        
        // Test toàn bộ tháng 8/2024
        for (int day = 1; day <= 31; day++) {
            try {
                LunarCalendar.LunarDate lunarDate = LunarCalendar.getLunarDate(day, 8, 2024);
                System.out.println("Solar: " + day + "/8/2024 -> Lunar: " + lunarDate.toString());
                
                // Kiểm tra tính hợp lệ
                assertTrue("Lunar day should be positive", lunarDate.day > 0);
                assertTrue("Lunar month should be between 1-12", lunarDate.month >= 1 && lunarDate.month <= 12);
                
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid date: " + day + "/8/2024 - " + e.getMessage());
            }
        }
    }
} 