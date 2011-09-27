package com.springone.myrestaurants.web;

import java.util.List;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.springone.myrestaurants.domain.Restaurant;
import com.springone.myrestaurants.domain.UserAccount;

@RequestMapping("/restaurants")
@Controller
public class RestaurantController extends BaseApplicationController {
	

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long id, Model model) {
        model.addAttribute("restaurant", restaurantRepository.findRestaurant(id));
        model.addAttribute("itemId", id);
        return "restaurants/show";
    }

	@RequestMapping(method = RequestMethod.GET)
    public String list(@RequestParam(value = "page", required = false) Integer page, 
    				   @RequestParam(value = "size", required = false) Integer size, 
    				   Model model) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            List<Restaurant> rests = restaurantRepository.findRestaurantEntries(page == null ? 0 : (page.intValue() - 1) * sizeNo, sizeNo);
            model.addAttribute("restaurants", rests);
            float nrOfPages = (float) restaurantRepository.countRestaurants() / sizeNo;
            model.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
        	 List<Restaurant> rests = restaurantRepository.findAllRestaurants();
            model.addAttribute("restaurants", rests);
        }
        return "restaurants/list";
    }
	
	@RequestMapping(value = "/{id}/{userId}", params = "favorite", method = RequestMethod.PUT)
    public String addFavoriteRestaurant(@PathVariable("id") Long id, 
    									@PathVariable("userId") Long userId, 
    									Model model) {
		Restaurant restaurant = this.restaurantRepository.findRestaurant(id);
		UserAccount account = this.userAccountRepository.findUserAccount(userId);    
		account.getFavorites().add(restaurant);
		this.userAccountRepository.persist(account);
        addDateTimeFormatPatterns(model);       
        model.addAttribute("useraccount", account);
        model.addAttribute("itemId", id);
        return "redirect:/useraccounts/" + account.getId();
    }




	@InitBinder
    void registerConverters(WebDataBinder binder) {
        if (binder.getConversionService() instanceof GenericConversionService) {
            GenericConversionService conversionService = (GenericConversionService) binder.getConversionService();
            conversionService.addConverter(getRestaurantConverter());
        }
    }

}
