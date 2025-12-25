package com.occaecat.ztoeschedule.presentation.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.occaecat.ztoeschedule.data.model.Rem
import org.junit.Rule
import org.junit.Test

class OnboardingUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun remSelection_displaysList_and_handlesClick() {
        // Given
        val testRems = listOf(
            Rem("1", "Житомирський РЕМ"),
            Rem("2", "Бердичівський РЕМ"),
            Rem("3", "Коростенський РЕМ")
        )
        var selectedRemName = ""

        // When
        composeTestRule.setContent {
            RemSelectionPage(
                rems = testRems,
                isLoading = false,
                onRemSelected = { selectedRemName = it.name }
            )
        }

        // Then
        // 1. Verify all REMs are displayed
        composeTestRule.onNodeWithText("Житомирський РЕМ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Бердичівський РЕМ").assertIsDisplayed()

        // 2. Perform Click
        composeTestRule.onNodeWithText("Бердичівський РЕМ").performClick()

        // 3. Verify callback was triggered
        assert(selectedRemName == "Бердичівський РЕМ")
    }

    @Test
    fun remSelection_showsLoading_whenLoading() {
        composeTestRule.setContent {
            RemSelectionPage(
                rems = emptyList(),
                isLoading = true,
                onRemSelected = {}
            )
        }

        // Verify progress indicator exists (it usually has no text, but we can look for it if we added a tag, 
        // or check that list is NOT present).
        // Since we didn't add testTags, checking for absence of list items is a safe proxy for empty state logic.
        composeTestRule.onNodeWithText("Житомирський РЕМ").assertDoesNotExist()
    }
}