/*
	BenoitLib Tutorial 01: Getting Started with MandelHub.
*/

// *** Starting your MandelHub Instance ***

// Before using MandelHub the group has to decide whoever is
// the leader (only one person) and who follows the leader (the rest).
// Every user in MandelHub has the same privileges, but the leader's
// machine carries out some tasks, like managing the network and keeping tempo.

// To start a MandelHub as a Leader you just call the MandelHub:start method
// with your name as an additional String argument.
// By convention the MandelHub instance is assigned to m for easy access.

m = MandelHub.start("Tutorial-Leader");

// As a leader your MandelHub instance is created synchronously. When joining
// an existing MandelHub session you need to wait for the first message from
// the leader. After connection the instance will be available
// via MandelHub.instance. It's probably a good idea to assign the instance
// to the interpreter variable m truogh the action argument, which specifies a 
// function that is called after a successful connect.

// (don't do this if you're already the leader)
MandelHub.join("Tutorial-Follower", action: {m = MandelHub.instance});

// MandelHub normally manages ports automatically. But if the current leader 
// isn't listening either on 57120 or the followers NetAddr.langPort the leaders
// listening port has to be specified.

// (don't do this if you're already the leader)
MandelHub.join("Tutorial-Follower", 57121, action: {m = MandelHub.instance});

// Off course this is all code that should be prepared before a performance.

// After MandelHub is started your Default TempoClock is replaced by a new
// TempoClock under MandelHubs control. You and your fellows now should have
// a synchronized clock.



// *** Taking the Lead! ***

// Every follower can take the lead at any time. This is most important, if the
// leader isn't responding anymore (i.e. he crashed) or left MandelHub. 

// (only makes sense if you're a follower)
m.takeLead;

// *** A GUI? Neat! ***

// Some functions of MandelHub can be exposed by a GUI. It also helps as a 
// visual cue for the timing and tempo.

m.gui;

// You see the Chat and Shout buttons? They make it easy to communicate with 
// your fellow live coders.



// *** Chatting and Shouting ***

// Chatting and Shouting differ in the visibility of the posted messages. Chat
// messages are only posted, while Shouted messages are displayed more distinct.
// Currently this is done with Growl on Mac OS X and notify-send on Linux.
// Install these tools if you want to have a nice visual cue for the messages.

m.chat("Hi, I'm chatting!"); // displayed in post window
m.shout("Hi, I'm shouting!"); // displayed in post window + notification

// The small input window on the GUI can be created by code as well. It's good
// to have this assigned to hot keys for fast access.

m.chatWindow;
m.shoutWindow;



// *** Changing the tempo ***

// Everybody can change the tempo (not only the leader). This can be done either
// in an instant or with a linear transition over time. The tempo is set in BPS.
// (observe on GUI)

m.changeTempo(90/60); // now 90 BPM
m.changeTempo(140/60, 30); // linear transition to 140BPM over 30 seconds



// *** Cleanin up ***

// MandelHub is designed as a singleton. If you need to restart your MandelHub
// you first need to clear the old one. Off course you don't want to do that 
// at the moment, as you want to continue with part 2 of this tutorial.

m.clear;