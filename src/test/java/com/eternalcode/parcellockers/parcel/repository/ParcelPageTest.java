package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.shared.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParcelPageTest {


    @Test
    @DisplayName("Should throw an exception when trying to go back from the first page")
    void previousWhenCurrentPageIsFirstThenThrowException() {
        Page parcelPage = new Page(0, 10);

        assertThrows(IllegalStateException.class, parcelPage::previous);
    }

    @Test
    @DisplayName("Should return the previous page when the current page is not the first page")
    void previousWhenCurrentPageIsNotFirst() {
        Page parcelPage = new Page(2, 10);
        Page previousPage = parcelPage.previous();

        assertEquals(1, previousPage.page());
        assertEquals(10, previousPage.size());
    }

    @Test
    @DisplayName("Should return zero offset when page is zero regardless of size")
    void getOffsetWhenPageIsZero() {
        Page parcelPage = new Page(0, 10);
        int expectedOffset = 0;

        int actualOffset = parcelPage.getOffset();

        assertEquals(expectedOffset, actualOffset);
    }

    @Test
    @DisplayName("Should return correct offset when page and size are positive")
    void getOffsetWhenPageAndSizeArePositive() {
        Page parcelPage = new Page(2, 10);
        int expectedOffset = 20;

        int actualOffset = parcelPage.getOffset();

        assertEquals(expectedOffset, actualOffset);
    }

    @Test
    @DisplayName("Should return false when the page number is 0")
    void hasPreviousWhenPageNumberIsZero() {
        Page parcelPage = new Page(0, 10);
        assertFalse(parcelPage.hasPrevious());
    }

    @Test
    @DisplayName("Should return true when the page number is greater than 0")
    void hasPreviousWhenPageNumberIsGreaterThanZero() {
        Page parcelPage = new Page(1, 10);
        assertTrue(parcelPage.hasPrevious());
    }
}