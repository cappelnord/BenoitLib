/*
	BenoitLib Tutorial 05: MacroExpander
	
	This is only for SuperCollider.app on Mac OS X.
*/

// MacroExpander is a neat tool to prepare your live coding sessions. 
// It allows for quick retreival of code snippets by the means of code.
// You should add all your code snippets in your startup file, so they're
// loaded automatically.



// *** Storing a code snippet ***

// All code snippets are stored with a String keyword.

MacroExpander.put("sine", "{SinOsc.ar(440)}.play;");



// *** Expanding a snippet ***

// You can now expand a short command to your stored code anywhere in 
// your code document:

"sine".xx

// Neat, ey? Keep in mind that you have to escape some characters in the
// String (you also have to escape the escape character!)

MacroExpander.put("escape", "(instrument: \\default, string: \"OK!\").play;");

"escape".xx



// *** Placing a cursor ***

// Sometimes it's useful to store a cursor position in your snippet (because you
// know, that you want to continue from this position). You can do this with %.
// Sorry, this also means you currently can't use modulo in your snippets.
// (bad design decision.)

MacroExpander.put("cursor", "{SinOsc.ar(%).dup}.play;");

"cursor".xx



// *** Snippet arguments ***

// You can add placeholders in your snippet that can be filled with values
// specified in your expand call.

MacroExpander.put("args", "{SinOsc.ar(#0#).dup * #1#}.play;");

// The # character is the delimiter for the arguments

"args#440#0.1".xx



// *** Functions as snippets ***

// You can also use a Function instead of a String. The function gets evaluated
// with a list of the specified arguments and the return value is inserted into
// the document.

(
MacroExpander.put("func", {|args|
    var ret = "// Whadup?\n";
    args[1].asInteger.do {
        ret = ret ++ (args[0] ++ "\n");	
    };
    ret;
});
)

"func#{SinOsc.ar(440)}.play;#10".xx