package com.springone.myrestaurants.web;

import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.springone.myrestaurants.data.RestaurantRepository;
import com.springone.myrestaurants.data.UserAccountRepository;
import com.springone.myrestaurants.domain.Restaurant;
import com.springone.myrestaurants.domain.UserAccount;

@Transactional
public class BaseApplicationController {

	@Autowired
	RestaurantRepository restaurantRepository;

	@Autowired
	UserAccountRepository userAccountRepository;

	@ModelAttribute("currentUserAccountId")
	public Long populateModelWithCurrentUserAccountIdAsLong() {
		UserAccount userAccount = getCurrentUserAccount();
		if (userAccount != null) {
			return userAccount.getId();
		} else {
			return -1L;
		}
	}

	private UserAccount getCurrentUserAccount() {
		String currentUser = SecurityContextHolder.getContext()
				.getAuthentication().getName();
		UserAccount userAccount = userAccountRepository.findByName(currentUser);
		if (userAccount != null) {
			userAccount.persist();
		}
		return userAccount;
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
