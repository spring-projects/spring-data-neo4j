$(document).ready(function() {

	 $.ajaxSetup ({  
		    cache: false  
		});
	 
	    var jsonUrl = "api/todos";
	    
        $.getJSON(  
            jsonUrl,  
            function(json) {  
        	  var items = [];

        	  $.each(json, function(i, todo) {
        	    items.push('<li>' + todo.title + '</li>');
        	  });

        	  $('<ul/>', {
        		    'class': 'disc',
        	    html: items.join('')
        	  }).appendTo('div#todos');
            }  
        );  

});