import importlib.util
import unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('docs',Path(__file__).parents[1]/'docs.py')
docs=importlib.util.module_from_spec(spec);spec.loader.exec_module(docs)

class DeclarationTests(unittest.TestCase):
    def test_defaults_callbacks_companions_and_hidden_bodies(self):
        source='''package test
private class Hidden {
    fun nope() { }
}
data class Preset(val points: Int = 5) {
    companion object {
        val Default = Preset(7)
    }
    private val secret = "{ not a block }"
    fun copyValue(value: Int = this.points): Preset = Preset(value)
}
@Composable
fun Camera(onResult: (Int) -> Unit = { _ -> }, key: String) {
    val notAnApi = 1
}
@Composable
fun Camera(key: String) { }
'''
        result=docs.declarations(source)
        self.assertNotIn('nope',result)
        self.assertNotIn('notAnApi',result)
        self.assertNotIn('secret',result)
        self.assertIn('companion object',result)
        self.assertIn('val Default = Preset(7)',result)
        self.assertIn('onResult: (Int) -> Unit = { _ -> }',result)
        self.assertEqual(result.count('@Composable'),2)
        self.assertIn('value: Int = this.points',result)

    def test_multiline_property_initializer(self):
        result=docs.declarations('class Validator {\n    val configurationKey: String =\n        "aspect-ratio"\n}\n')
        self.assertIn('"aspect-ratio"',result)
        self.assertTrue(result.rstrip().endswith('}'))

    def test_overloads_and_enum_values(self):
        source='''enum class State { First, Second }
fun similar(a: Long, b: Long): Boolean { return a == b }
fun similar(a: String, b: String): Boolean = a == b
'''
        result=docs.declarations(source)
        self.assertIn('First, Second',result)
        self.assertEqual(result.count('fun similar'),2)
        self.assertNotIn('return a',result)
