<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div>
    <h3>Welcome to Cineasts.net</h3>
   	<div id="big-search-wrap">
        <form action="/movies" method="get">
	        <input type="text" class="big-search" name="q" value="Find movie" onfocus="this.value='';" onblur="this.value = (this.value=='') ? 'Find movie' : this.value;" />
	        <input type="submit" class="big-search-submit" value="Search"/>
        </form>
    </div>
</div>