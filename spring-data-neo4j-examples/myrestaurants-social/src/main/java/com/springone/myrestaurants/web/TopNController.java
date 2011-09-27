package com.springone.myrestaurants.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.springone.myrestaurants.domain.RatedRestaurant;
import com.springone.myrestaurants.domain.Recommendation;
import com.springone.myrestaurants.domain.UserAccount;

@RequestMapping("/topn")
@Controller
public class TopNController extends BaseApplicationController {
	
	
    @RequestMapping(method = RequestMethod.GET)
    public String list(@ModelAttribute("currentUserAccountId") Long userId,
    		           Model model) {
    	UserAccount currentUser = this.userAccountRepository.findUserAccount(userId);
    	Collection<RatedRestaurant> top5 = currentUser.getTop5RatedRestaurants();
    	List<RatedRestaurantBean> topn = new ArrayList<RatedRestaurantBean>();
    	for (RatedRestaurant rr : top5) {
    		RatedRestaurantBean rrb = new RatedRestaurantBean();
    		rrb.setId(rr.getRestaurant().getId());
    		rrb.setName(rr.getRestaurant().getName());
    		float totPoints = 0;
    		long count = 0;
    		for (Recommendation r : rr.getRecommendations()) {
    			totPoints += r.getStars();
    			count++;
    		}
    		rrb.setRating(totPoints / count);
    		rrb.setRecommendations(count);
    		topn.add(rrb);
    	}
    	model.addAttribute("topn", topn);
    	
        return "topn/list";
    }
    
    Converter<UserAccount, String> getUserAccountConverter() {
        return new Converter<UserAccount, String>() {
            public String convert(UserAccount friend) {
                return new StringBuilder().append(friend.getUserName()).toString();
            }
        };
    }
    
    @InitBinder
    void registerConverters(WebDataBinder binder) {
        if (binder.getConversionService() instanceof GenericConversionService) {
            GenericConversionService conversionService = (GenericConversionService) binder.getConversionService();
            conversionService.addConverter(getUserAccountConverter());
        }
    }


}
