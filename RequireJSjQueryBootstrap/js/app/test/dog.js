define(['./cat'], function(cat){
	
	var heart = "I'm dog. I love cat.";
	console.log(heart);
	if (cat == null) {
		console.log("Cat is asleep.");
	} else {
		console.log("Cat is an object.");
	}
	return heart;
	
});