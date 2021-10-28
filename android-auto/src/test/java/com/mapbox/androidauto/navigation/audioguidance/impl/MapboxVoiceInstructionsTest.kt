package com.mapbox.androidauto.navigation.audioguidance.impl

import com.mapbox.androidauto.testing.MainCoroutineRule
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

@ExperimentalCoroutinesApi
class MapboxVoiceInstructionsTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
    private val carAppVoiceInstructions = MapboxVoiceInstructions(mapboxNavigation)

    @Test
    fun `should emit voice instruction`(): Unit = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(mockk {
                every { routes } returns listOf(mockk(), mockk())
            })
        }
        every { mapboxNavigation.registerVoiceInstructionsObserver(any()) } answers {
            firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(mockk {
                every { announcement() } returns "Left on Broadway"
            })
        }

        val state = carAppVoiceInstructions.voiceInstructions().take(2).toList()

        assertTrue(state[0].isPlayable)
        assertEquals("Left on Broadway", state[1].voiceInstructions?.announcement())
    }

    @Test
    fun `should emit multiple voice instructions`(): Unit = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(mockk {
                every { routes } returns listOf(mockk(), mockk())
            })
        }
        every { mapboxNavigation.registerVoiceInstructionsObserver(any()) } answers {
            firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(mockk {
                every { announcement() } returns "Left on Broadway"
            })
            firstArg<VoiceInstructionsObserver>().onNewVoiceInstructions(mockk {
                every { announcement() } returns "Right on Pennsylvania"
            })
        }

        val voiceInstruction = carAppVoiceInstructions.voiceInstructions().take(3).toList()

        val actual = voiceInstruction.map { it.voiceInstructions?.announcement() }
        assertEquals("Left on Broadway", actual[1])
        assertEquals("Right on Pennsylvania", actual[2])
    }

    @Test
    fun `should emit null routes is empty`(): Unit = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STARTED
            )
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(mockk {
                every { routes } returns emptyList()
            })
        }

        val state = carAppVoiceInstructions.voiceInstructions().first()

        assertNull(state.voiceInstructions?.announcement())
    }

    @Test
    fun `should emit null when session is stopped`(): Unit = coroutineRule.runBlockingTest {
        every { mapboxNavigation.registerTripSessionStateObserver(any()) } answers {
            firstArg<TripSessionStateObserver>().onSessionStateChanged(
                TripSessionState.STOPPED
            )
        }

        val state = carAppVoiceInstructions.voiceInstructions().first()

        assertNull(state.voiceInstructions?.announcement())
    }
}