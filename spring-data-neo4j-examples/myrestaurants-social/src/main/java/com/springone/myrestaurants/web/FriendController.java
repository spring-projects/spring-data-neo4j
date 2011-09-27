package com.springone.myrestaurants.web;

import java.util.Set;

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
import org.springframework.web.bind.annotation.RequestParam;

import com.springone.myrestaurants.domain.UserAccount;

@RequestMapping("/friends")
@Controller
public class FriendController extends BaseApplicationController {
	
	
    @RequestMapping(method = RequestMethod.POST)
    public String create(FriendFormBean friend, 
    					 @ModelAttribute("currentUserAccountId") Long userId,
    					 BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("friend", friend);
            return "friends/create";
        }
        //TODO additional error checking
        UserAccount account = this.userAccountRepository.findUserAccount(userId);
        UserAccount friendAccount = this.userAccountRepository.findByName(friend.getUserName());
        if (friendAccount != null) {
        	account.getFriends().add(friendAccount);
        }
        return "redirect:/friends/" + friendAccount.getId();
    }

    @RequestMapping(params = "form", method = RequestMethod.GET)
    public String createForm(Model model) {
        model.addAttribute("friend", new FriendFormBean());
        return "friends/create";
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long id, Model model) {
    	UserAccount friendAccount = this.userAccountRepository.findUserAccount(id);
        model.addAttribute("friend", friendAccount);
        model.addAttribute("itemId", id);
        return "friends/show";
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public String list(@RequestParam(value = "page", required = false) Integer page, 
    		           @RequestParam(value = "size", required = false) Integer size, 
    		           @ModelAttribute("currentUserAccountId") Long userId,
    		           Model model) {
    	
    	UserAccount currentUser = this.userAccountRepository.findUserAccount(userId);
    	Set<UserAccount> friends = currentUser.getFriends();
    	model.addAttribute("friends", friends);
    	
        return "friends/list";
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public String delete(@PathVariable("id") Long id, 
    			         @ModelAttribute("currentUserAccountId") Long userId,
    		             @RequestParam(value = "page", required = false) Integer page, 
    		             @RequestParam(value = "size", required = false) Integer size, Model model) {
    	UserAccount account = this.userAccountRepository.findUserAccount(userId);
    	UserAccount friendAccount = this.userAccountRepository.findUserAccount(id);
    	if (account.getFriends().contains(friendAccount)) {
    		account.getFriends().remove(friendAccount);
    	}
        model.addAttribute("page", (page == null) ? "1" : page.toString());
        model.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/friends?page=" + ((page == null) ? "1" : page.toString()) + "&size=" + ((size == null) ? "10" : size.toString());
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
