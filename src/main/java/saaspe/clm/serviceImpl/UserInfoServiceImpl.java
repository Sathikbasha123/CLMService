package saaspe.clm.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import saaspe.clm.constant.Constant;
import saaspe.clm.document.UsersInfoDocument;
import saaspe.clm.model.*;
import saaspe.clm.repository.UserInfoRespository;
import saaspe.clm.service.MailSenderService;
import saaspe.clm.service.UserInfoService;
import saaspe.clm.utills.RedisUtility;

import javax.mail.MessagingException;

@Service
public class UserInfoServiceImpl implements UserInfoService {


    @Autowired
    private UserInfoRespository userInfoRespository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    private RedisUtility redisUtility;

    @Autowired
    private MailSenderService mailSenderService;

    public UserInfoServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public CommonResponse activateUser(String uniqueId, String email) {
        UsersInfoDocument userDocument = userInfoRespository.findByUniqueId(uniqueId);
        if (!userDocument.isActive()) {
            userDocument.setActive(true);
            userDocument.setUpdatedOn(new Date());
            userDocument.setUpdatedBy(email);
            userInfoRespository.save(userDocument);
            try {
                mailSenderService.sendMailToUserIsActivated(userDocument.getEmail(), userDocument.getName(), userDocument.getDivision(), userDocument.getRoles());
            } catch (IOException | TemplateException | MessagingException e1) {
                e1.printStackTrace();
            }
            return new CommonResponse(HttpStatus.CREATED, new Response("ActiveUserResponse", new ArrayList<>()), "User Activated Successfully!");
        } else {
            return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("ActiveUserResponse", new ArrayList<>()), "User is already active!");
        }
    }

    @Override
    public CommonResponse deActivateUser(String uniqueId, String email) {
        UsersInfoDocument userDocument = userInfoRespository.findByUniqueId(uniqueId);
        if (userDocument.isActive()) {
            userDocument.setActive(false);
            userDocument.setDeactivatedDate(new Date());
            userDocument.setDeactivatedBy(email);
            userDocument.setUpdatedOn(new Date());
            userDocument.setUpdatedBy(email);
            userInfoRespository.save(userDocument);
            redisUtility.deleteKeyformredis(Constant.AZURE+userDocument.getEmail());
            String admin =  userInfoRespository.findByEmail(email).getCurrentRole();
            try {
                mailSenderService.sendMailToUserIsDeactivated(userDocument.getEmail(), userDocument.getName(), userDocument.getDivision(), userDocument.getRoles(),admin);
            } catch (IOException | TemplateException | MessagingException e1) {
                e1.printStackTrace();
            }
            return new CommonResponse(HttpStatus.CREATED, new Response("ActiveUserResponse", new ArrayList<>()), "User De-activated Successfully!");
        } else {
            return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("ActiveUserResponse", new ArrayList<>()), "User is already De-activated");
        }
    }

    @Override
    public CommonResponse userListView(int page, int limit, String searchText, String email, String order, String orderBy, Boolean isActive, String createdThrough, List<String> division) {
        Sort sort = null;
        PageRequest pageable = null;
        List<UsersInfoDocument> list = null;
        if (order != null && !order.isEmpty()) {
            Sort.Direction sortDirection = Sort.Direction.ASC;
            if (order.equalsIgnoreCase("desc")) {
                sortDirection = Sort.Direction.DESC;
            }
            sort = Sort.by(sortDirection, orderBy);
            pageable = PageRequest.of(page, limit, sort);
        }
        long totalCount=0;
        Query query = new Query();
        query.addCriteria(Criteria.where("isActive").is(isActive));
        query.addCriteria(Criteria.where("email").ne(email));
        Criteria criteria = new Criteria();
        if(searchText!=null&&!searchText.isEmpty()) {
            criteria.orOperator(
                    Criteria.where("name").regex(searchText, "i"),
                    Criteria.where("email").regex(searchText, "i")
            );
            query.addCriteria(criteria);
        }
        if (division != null && !division.isEmpty()) {
            query.addCriteria(Criteria.where("division").in(division));
        }
        if (createdThrough != null && !createdThrough.isBlank()) {
            query.addCriteria(Criteria.where("createdThrough").regex(createdThrough, "i"));
        }
        Pageable pageableObject = pageable;
        totalCount = mongoTemplate.count(query,UsersInfoDocument.class);
        if(order!=null){
            query.with(pageableObject);
        }
        list = mongoTemplate.find(query,UsersInfoDocument.class).stream().collect(Collectors.toList());
        List<UserListResponse> userListResponses = list.stream().map(usersInfoDocument -> {
            UserListResponse userListResponse = new UserListResponse();
            userListResponse.setName(usersInfoDocument.getName());
            userListResponse.setUniqueId(usersInfoDocument.getUniqueId());
            userListResponse.setEmail(usersInfoDocument.getEmail());
            userListResponse.setDivision(usersInfoDocument.getDivision());
            userListResponse.setRoles(usersInfoDocument.getRoles());
            if (usersInfoDocument.getUpdatedOn() != null) {
                userListResponse.setUpdatedOn(usersInfoDocument.getUpdatedOn());
            }
            if (usersInfoDocument.getApprovedBy() != null) {
                userListResponse.setUpdatedBy(usersInfoDocument.getApprovedBy());
            }
            if (usersInfoDocument.getUpdatedBy() != null) {
                userListResponse.setUpdatedBy(usersInfoDocument.getUpdatedBy());
            }
            userListResponse.setCreatedThrough(usersInfoDocument.getCreatedThrough());
            userListResponse.setCreatedOn(usersInfoDocument.getCreatedOn());
            userListResponse.setCreatedBy(usersInfoDocument.getCreatedBy());
            userListResponse.setUpdatedOn(usersInfoDocument.getUpdatedOn());
            userListResponse.setActive(usersInfoDocument.isActive());
            return userListResponse;
        }).collect(Collectors.toList());
        UserListPagination userswithpagination = new UserListPagination();
        userswithpagination.setTotal(totalCount);
        userswithpagination.setRecords(userListResponses);
        return new CommonResponse(HttpStatus.OK, new Response("UserListViewResponse", userswithpagination), "List of users retrieved successfully!");
    }

    @Override
    public CommonResponse editUserRoles(EditUserRolesRequest editUserRolesRequest, String email) {
        UsersInfoDocument userDocument = userInfoRespository.findByUniqueId(editUserRolesRequest.getUniqueId());
        if (userDocument != null) {
            userDocument.getRoles().clear();
            List<String> roles = new ArrayList<>();
            for (String role : editUserRolesRequest.getNewRoles()) {
                roles.add(role);
            }
            userDocument.setCurrentRole(roles.get(0));
            userDocument.setUpdatedBy(email);
            userDocument.setUpdatedOn(new Date());
            userDocument.setRoles(roles);
            userInfoRespository.save(userDocument);
            try {
                mailSenderService.sendMailToUserRoleUpdated(userDocument.getEmail(), userDocument.getName(), userDocument.getDivision(), userDocument.getRoles());
            } catch (IOException | TemplateException | MessagingException e1) {
                e1.printStackTrace();
            }
            return new CommonResponse(HttpStatus.OK, new Response("EditUserRoleResponse", new ArrayList<>()), "User roles updated successfully!");
        } else {
            return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("EditUserRoleResponse", new ArrayList<>()), "User not found with the provided unique ID!");
        }
    }

    @Override
    public CommonResponse updateCurrentUserRoles(CurrentUserRoleUpdateRequest userRoleUpdateRequest, String email){
        UsersInfoDocument userDocument = userInfoRespository.findByEmail(email);
        if (userDocument != null) {
            List<String> roles = userDocument.getRoles();
            if(!roles.contains(userRoleUpdateRequest.getCurrentRole())){
                return new CommonResponse(HttpStatus.BAD_REQUEST,new Response("User Current Roles does not matcht the existing roles",userRoleUpdateRequest.getCurrentRole()),"User Current Roles does not matcht the existing roles");
            }
            userDocument.setCurrentRole(userRoleUpdateRequest.getCurrentRole());
            userDocument.setUpdatedBy(email);
            userDocument.setUpdatedOn(new Date());
            userInfoRespository.save(userDocument);
            return new CommonResponse(HttpStatus.OK, new Response("Update User Current Role Response", new ArrayList<>()), "User current roles updated successfully!");
        } else {
            return new CommonResponse(HttpStatus.OK, new Response("Update User Current Role Response", new ArrayList<>()), "User not found with the provided unique ID!");
        }
    }

    @Override
    public CommonResponse getCurrentUserRoles(String email){
        UsersInfoDocument userDocument = userInfoRespository.findByEmail(email);
        UserCurrentRoleResponse userCurrentRoleResponse = new UserCurrentRoleResponse();
        if (userDocument != null) {
            userCurrentRoleResponse.setUniqueId(userDocument.getUniqueId());
            userCurrentRoleResponse.setCurrentRole(userDocument.getCurrentRole());
            return new CommonResponse(HttpStatus.OK, new Response("User Current Role Response",userCurrentRoleResponse), "User current roles fetched successfully!");
        } else {
            return new CommonResponse(HttpStatus.OK, new Response("User Current Role Response", userCurrentRoleResponse), "User not found with the provided unique ID!");
        }
    }
    @Override
    public 	CommonResponse getUserRoles(String email) {
        UsersInfoDocument userDocument = userInfoRespository.findByEmail(email);
        UserRoleResponse userRoleResponse = new UserRoleResponse();
        if (userDocument != null) {
            userRoleResponse.setUniqueId(userDocument.getUniqueId());
            userRoleResponse.setRoles(userDocument.getRoles());
            userRoleResponse.setDivision(userDocument.getDivision());
            return new CommonResponse(HttpStatus.OK, new Response("User Role Response", userRoleResponse), "User roles fetched successfully!");
        } else {
            return new CommonResponse(HttpStatus.OK, new Response("User Role Response", userRoleResponse), "User not found with the provided unique ID!");
        }
    }

    @Override
    public CommonResponse editUserDivisionAndRoles(EditUserRolesRequest editUserRolesRequest, String email) {
        UsersInfoDocument userDocument = userInfoRespository.findByUniqueId(editUserRolesRequest.getUniqueId());
        if (userDocument != null) {
            userDocument.getRoles().clear();
            List<String> roles = new ArrayList<>();
            if(editUserRolesRequest.getNewRoles()==null || editUserRolesRequest.getNewRoles().isEmpty()){
                return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("EditUserRoleResponse", new ArrayList<>()), "User must have atleast one role");
            }
            for (String role : editUserRolesRequest.getNewRoles()) {
                roles.add(role);
            }
            userDocument.setDivision(editUserRolesRequest.getDivision());
            userDocument.setCurrentRole(roles.get(0));
            userDocument.setUpdatedBy(email);
            userDocument.setUpdatedOn(new Date());
            userDocument.setRoles(roles);
            userInfoRespository.save(userDocument);
            try {
                mailSenderService.sendMailToUserRoleUpdated(userDocument.getEmail(), userDocument.getName(), userDocument.getDivision(), userDocument.getRoles());
            } catch (IOException | TemplateException | MessagingException e1) {
                e1.printStackTrace();
            }
            return new CommonResponse(HttpStatus.OK, new Response("EditUserRoleResponse", new ArrayList<>()), "User roles updated successfully!");
        } else {
            return new CommonResponse(HttpStatus.BAD_REQUEST, new Response("EditUserRoleResponse", new ArrayList<>()), "User not found with the provided unique ID!");
        }
    }
}