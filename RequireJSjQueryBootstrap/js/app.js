requirejs.config({
    "baseUrl": "js/lib",
    "paths": {
      "app": "../app"
    },
    "shim": {
        "bootstrap": ["jquery"]
    }
});

// Load the main app module to start the app
requirejs(["app/main"]);


// Test
requirejs(['app/test/pig'], function(pig){
	console.log("cat:" + pig.cat + " dog:" + pig.dog + " pig:" + pig.pig);
});

