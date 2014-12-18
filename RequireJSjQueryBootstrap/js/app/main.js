define(['jquery', 'bootstrap'], function($){

    // DOM ready
    $(function(){
    	console.log("main.js Enter.");
        // Twitter Bootstrap 3 carousel plugin
        $("#carousel-example-generic").carousel({ interval: 2000, cycle: true });
    });
});
