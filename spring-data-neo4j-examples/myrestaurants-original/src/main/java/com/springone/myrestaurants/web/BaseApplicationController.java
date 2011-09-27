package com.springone.myrestaurants.web;

import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.springone.myrestaurants.dao.RestaurantDao;
import com.springone.myrestaurants.dao.UserAccountDao;
import com.springone.myrestaurants.domain.Restaurant;
import com.springone.myrestaurants.domain.UserAccount;

public class BaseApplicationController {

	@Autowired
	RestaurantDao restaurantDao;

	@Autowired
	UserAccountDao userAccountDao;

	@ModelAttribute("currentUserAccountId")
	public String populateCurrentUserName() {
		String currentUser = SecurityContextHolder.getContext()
				.getAuthentication().getName();
		UserAccount userAccount = userAccountDao.findByName(currentUser);
		if (userAccount != null) {
			return userAccount.getId().toString();
		} else {
			return "USER-ID-NOT-AVAILABLE";
		}
	}

	void addDateTimeFormatPatterns(Model model) {
		model.addAttribute(
				"userAccount_birthdate_date_format",
				DateTimeFormat.patternForStyle("S-",
						LocaleContextHolder.getLocale()));
	}

	protected Converter<Restaurant, String> getRestaurantConverter() {
		return new Converter<Restaurant, String>() {
			public String convert(Restaurant restaurant) {
				return new StringBuilder().append(restaurant.getName())
						.append(" ").append(restaurant.getCity()).append(" ")
						.append(restaurant.getState()).toString();
			}
		};
	}

}
