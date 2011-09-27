package com.springone.myrestaurants.web;

import java.util.ArrayList;
import java.util.List;

import com.springone.myrestaurants.domain.Recommendation;
import com.springone.myrestaurants.domain.Restaurant;
import com.springone.myrestaurants.domain.UserAccount;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;

@RequestMapping("/recommendations")
@Controller
public class RecommendationController extends BaseApplicationController {

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long recommendationId,
    				   @ModelAttribute("currentUserAccountId") Long userId,
    				   Model model) {
		
		Recommendation foundRec = findRecommendation(userId, recommendationId);
		RecommendationFormBean bean = new RecommendationFormBean();
		if (foundRec != null) {
			bean.setComments(foundRec.getComment());
			bean.setRating(foundRec.getStars());
            bean.setId(foundRec.getRelationshipId());
            Restaurant r = foundRec.getRestaurant();
            bean.setName(r.getName());
            bean.setRestaurantId(r.getId());
		}
		model.addAttribute("recommendation", bean);
        return "recommendations/show";
    }


	private Recommendation findRecommendation(Long userId,
			Long recommendationId) {
		UserAccount account = this.userAccountRepository.findUserAccount(userId);
		Iterable<Recommendation> recs = account.getRecommendations();
		Recommendation foundRec = null;
		for (Recommendation recommendation : recs) {
			if (recommendation.getRelationshipId().equals(recommendationId)) {
				foundRec = recommendation;
			}
		}
		return foundRec;
	}
	
	
	@RequestMapping(method = RequestMethod.GET)
    public String list(@RequestParam(value = "page", required = false) Integer page, 
    				   @RequestParam(value = "size", required = false) Integer size, 
    				   @ModelAttribute("currentUserAccountId") Long userId,
    				   Model model) {

		UserAccount account = this.userAccountRepository.findUserAccount(userId);
		Iterable<Recommendation> recs = account.getRecommendations();
		//View expects a list with indexer access and properties that match those of the form bean.
		List<RecommendationFormBean> listRecs = new ArrayList<RecommendationFormBean>();
		for (Recommendation recommendation : recs) {
			RecommendationFormBean rfb = new RecommendationFormBean();
            final Restaurant restaurant = recommendation.getRestaurant();
            rfb.setComments(recommendation.getComment());
            rfb.setName(restaurant.getName());
			rfb.setRating(recommendation.getStars());		
			rfb.setId(recommendation.getRelationshipId());
            rfb.setRestaurantId(restaurant.getId());
			listRecs.add(rfb);
		}		                    
		model.addAttribute("recommendations", listRecs);
        return "recommendations/list";
    }
	
	
	@RequestMapping(method = RequestMethod.POST)
	public String create(RecommendationFormBean recommendationFormBean,
						 @ModelAttribute("currentUserAccountId") Long userId,
						 BindingResult result,
						 Model model) {

		if (result.hasErrors()) {
			model.addAttribute("recommendation", recommendationFormBean);
			return "recommendations/create";
		}
		long restaurantId = recommendationFormBean.getRestaurantId();
		Restaurant restaurant = this.restaurantRepository.findRestaurant(restaurantId);
		UserAccount account = this.userAccountRepository.findUserAccount(userId);
		Recommendation recommendation = account.rate(restaurant,
				recommendationFormBean.getRating(),
				recommendationFormBean.getComments());
		model.addAttribute("recommendationId", recommendation.getRelationshipId());
		return "redirect:/recommendations/" + recommendation.getRelationshipId();
	}

	@RequestMapping(value = "/{restaurantId}/{userId}", params = "form", method = RequestMethod.GET)
    public String createForm(@PathVariable("restaurantId") Long restaurantId, 
			 				 @PathVariable("userId") Long userId,
			                 Model model) {   
		RecommendationFormBean recBean = new RecommendationFormBean();
		Restaurant restaurant = this.restaurantRepository.findRestaurant(restaurantId);
		recBean.setRestaurantId(restaurantId);
		recBean.setName(restaurant.getName());
        model.addAttribute("recommendation", recBean);              
        //currentUserId is part of the implicit model due to spring security
        
        //model.addAttribute("userId", userId.toString());
        return "recommendations/create"; ///" + restaurantId + "/" + userId;
    }
	
    @RequestMapping(method = RequestMethod.PUT)
    public String update(RecommendationFormBean recommendationFormBean, 
    					 @ModelAttribute("currentUserAccountId") Long userId,
    				     BindingResult result, 
    				     Model model) {
        if (result.hasErrors()) {
            model.addAttribute("recommendation", recommendationFormBean);
            return "recommendations/update";
        }
        Recommendation foundRec = findRecommendation(userId, recommendationFormBean.getId());
        foundRec.rate(recommendationFormBean.getRating(), recommendationFormBean.getComments());  
        model.addAttribute("itemId", recommendationFormBean.getId());
        return "redirect:/recommendations/" + recommendationFormBean.getId();
    }
	
    @RequestMapping(value = "/{id}", params = "form", method = RequestMethod.GET)
    public String updateForm(@PathVariable("id") Long id, 
    						 @ModelAttribute("currentUserAccountId") Long userId,
    						 Model model) {
    	Recommendation foundRec = findRecommendation(userId, id);
    	RecommendationFormBean recBean = new RecommendationFormBean();
    	if (foundRec != null) {
    	  recBean.setComments(foundRec.getComment());   
    	  recBean.setId(foundRec.getRelationshipId());
    	  recBean.setRating(foundRec.getStars());        	 
    	  recBean.setName(foundRec.getRestaurant().getName());
    	  recBean.setRestaurantId(foundRec.getRestaurant().getId());
    	}
        model.addAttribute("recommendation", recBean);
        model.addAttribute("itemId", recBean.getId());
        return "recommendations/update";
    }
	
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public String delete(@PathVariable("id") Long id, 
    				     @RequestParam(value = "page", required = false) Integer page, 
    					 @RequestParam(value = "size", required = false) Integer size, 
    					 @ModelAttribute("currentUserAccountId") Long userId,
    					 Model model) {
    	Recommendation foundRec = findRecommendation(userId, id);
    	if (foundRec != null) {
    		if (foundRec.hasPersistentState()) {
    			foundRec.getPersistentState().delete();
    		}
    	}
        model.addAttribute("page", (page == null) ? "1" : page.toString());
        model.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/recommendations?page=" + ((page == null) ? "1" : page.toString()) + "&size=" + ((size == null) ? "10" : size.toString());
    }
    

	
	
}
