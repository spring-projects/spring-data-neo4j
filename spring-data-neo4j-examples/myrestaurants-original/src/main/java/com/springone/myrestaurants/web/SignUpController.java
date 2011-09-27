package com.springone.myrestaurants.web;

import java.util.Collection;

import javax.validation.Valid;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.springone.myrestaurants.domain.Restaurant;
import com.springone.myrestaurants.domain.UserAccount;

@RequestMapping("/useraccounts")
@Controller
public class SignUpController extends BaseApplicationController {


	@RequestMapping(method = RequestMethod.POST)
    public String create(@Valid UserAccount userAccount, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userAccount", userAccount);
            addDateTimeFormatPatterns(model);
            return "useraccounts/create";
        }
        userAccountDao.persist(userAccount);
        return "redirect:/useraccounts/" + userAccount.getId();
    }

	@RequestMapping(params = "form", method = RequestMethod.GET)
    public String createForm(Model model) {
        model.addAttribute("userAccount", new UserAccount());
        addDateTimeFormatPatterns(model);
        return "useraccounts/create";
    }
	
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long id, Model model) {
        addDateTimeFormatPatterns(model);
        model.addAttribute("useraccount", userAccountDao.findUserAccount(id));
        model.addAttribute("itemId", id);
        return "useraccounts/show";
    }

	@RequestMapping(method = RequestMethod.PUT)
    public String update(@Valid UserAccount userAccount, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userAccount", userAccount);
            addDateTimeFormatPatterns(model);
            return "useraccounts/update";
        }
        userAccountDao.merge(userAccount);
        return "redirect:/useraccounts/" + userAccount.getId();
    }

	@RequestMapping(value = "/{id}", params = "form", method = RequestMethod.GET)
    public String updateForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("userAccount", userAccountDao.findUserAccount(id));
        addDateTimeFormatPatterns(model);
        return "useraccounts/update";
    }
	
	@RequestMapping(value = "/{username}", params = "form2", method = RequestMethod.GET)
    public String updateForm(@PathVariable("username") String userName, Model model) {
		UserAccount userAccount = userAccountDao.findByName(userName);
        model.addAttribute("userAccount", userAccount);
        addDateTimeFormatPatterns(model);
        return "useraccounts/update";
    }
	


	@ModelAttribute("restaurants")
    public Collection<Restaurant> populateRestaurants() {
        return restaurantDao.findAllRestaurants();
    }


	Converter<String, Restaurant> getRestaurantConverterFromString() {
        return new Converter<String, Restaurant>() {
            public Restaurant convert(String id) {
                return restaurantDao.findRestaurant(Long.valueOf(id)); 
            }
        };
    }

	Converter<UserAccount, String> getUserAccountConverter() {
        return new Converter<UserAccount, String>() {
            public String convert(UserAccount userAccount) {
                return new StringBuilder().append(userAccount.getUserName()).append(" ").append(userAccount.getFirstName()).append(" ").append(userAccount.getLastName()).toString();
            }
        };
    }

	@InitBinder
    void registerConverters(WebDataBinder binder) {
        if (binder.getConversionService() instanceof GenericConversionService) {
            GenericConversionService conversionService = (GenericConversionService) binder.getConversionService();
            conversionService.addConverter(getRestaurantConverter());
            conversionService.addConverter(getUserAccountConverter());
            conversionService.addConverter(getRestaurantConverterFromString());
        }
    }

}
