package com.bytecoder.lurora.backend.utils

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ErrorHandlerTest {
    
    private lateinit var errorHandler: ErrorHandler
    
    @Before
    fun setup() {
        errorHandler = ErrorHandler()
    }
    
    @Test
    fun `handleError should add error to list when showToUser is true`() {
        // Given
        val exception = RuntimeException("Test error")
        val context = "Test context"
        
        // When
        errorHandler.handleError(exception, context, showToUser = true)
        
        // Then
        val errors = errorHandler.errors.value
        assertEquals("Should have one error", 1, errors.size)
        assertEquals("Error message should match", "Test error", errors.first().message)
        assertEquals("Error context should match", context, errors.first().context)
    }
    
    @Test
    fun `handleError should not add error to list when showToUser is false`() {
        // Given
        val exception = RuntimeException("Test error")
        val context = "Test context"
        
        // When
        errorHandler.handleError(exception, context, showToUser = false)
        
        // Then
        val errors = errorHandler.errors.value
        assertEquals("Should have no errors", 0, errors.size)
    }
    
    @Test
    fun `clearError should remove specific error`() {
        // Given
        val exception1 = RuntimeException("Error 1")
        val exception2 = RuntimeException("Error 2")
        errorHandler.handleError(exception1, "Context 1")
        errorHandler.handleError(exception2, "Context 2")
        
        val errors = errorHandler.errors.value
        val errorToRemove = errors.first()
        
        // When
        errorHandler.clearError(errorToRemove)
        
        // Then
        val remainingErrors = errorHandler.errors.value
        assertEquals("Should have one error remaining", 1, remainingErrors.size)
        assertFalse("Removed error should not be in list", remainingErrors.contains(errorToRemove))
    }
    
    @Test
    fun `clearAllErrors should remove all errors`() {
        // Given
        errorHandler.handleError(RuntimeException("Error 1"), "Context 1")
        errorHandler.handleError(RuntimeException("Error 2"), "Context 2")
        
        // When
        errorHandler.clearAllErrors()
        
        // Then
        val errors = errorHandler.errors.value
        assertEquals("Should have no errors", 0, errors.size)
    }
    
    @Test
    fun `safeCall should return success when action succeeds`() = runTest {
        // Given
        val expectedResult = "Success"
        
        // When
        val result = errorHandler.safeCall("Test context") {
            expectedResult
        }
        
        // Then
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Result should match expected", expectedResult, result.getOrNull())
    }
    
    @Test
    fun `safeCall should return failure when action throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Test error")
        
        // When
        val result = errorHandler.safeCall("Test context") {
            throw exception
        }
        
        // Then
        assertTrue("Result should be failure", result.isFailure)
        assertEquals("Exception should match", exception, result.exceptionOrNull())
    }
    
    @Test
    fun `safeTry should handle non-suspending functions`() {
        // Given
        val expectedResult = "Success"
        
        // When
        val result = errorHandler.safeTry("Test context") {
            expectedResult
        }
        
        // Then
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Result should match expected", expectedResult, result.getOrNull())
    }
    
    @Test
    fun `error list should not exceed maximum size`() {
        // Given - Add more than 10 errors
        repeat(15) { index ->
            errorHandler.handleError(RuntimeException("Error $index"), "Context $index")
        }
        
        // Then
        val errors = errorHandler.errors.value
        assertEquals("Should have maximum 10 errors", 10, errors.size)
    }
}