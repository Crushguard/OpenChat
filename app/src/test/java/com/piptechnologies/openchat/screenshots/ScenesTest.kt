package com.piptechnologies.openchat.screenshots

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The locale matrix renders exactly the English screenshots: 34 scenes (design map §6) with distinct names, each
 * rendered in English by the test method of the same name. CI names the English files after the methods and the
 * translated ones after the scenes, and expects the same 34 names in every language folder.
 */
class ScenesTest {
    @Test
    fun thirtyFourScenesWithDistinctNames() {
        val names = Scenes.all.map { it.name }
        assertEquals(34, names.size)
        assertEquals(names.distinct(), names)
    }

    @Test
    fun everySceneIsTheEnglishTestOfItsName() {
        val englishTests = listOf(
            LaunchScreenshotTests::class.java,
            HomeScreenshotTests::class.java,
            GateScreenshotTests::class.java,
            MessagesScreenshotTests::class.java,
            MediaScreenshotTests::class.java,
            SecondAccountScreenshotTests::class.java,
            SettingsScreenshotTests::class.java,
        ).flatMap { tests -> tests.methods.filter { it.isAnnotationPresent(Test::class.java) }.map { it.name } }
        assertEquals(Scenes.all.map { it.name }.sorted(), englishTests.sorted())
    }
}
