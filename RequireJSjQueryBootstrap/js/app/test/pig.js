define(['app/test/cat', './dog'], function(cat, dog){
	
	var heart = "I'm pic. I love cat&dog.";
	console.log(heart);
	
	return { 
			cat : cat,
			dog : dog,
			pig : heart
		};
});