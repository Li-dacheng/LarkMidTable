package com.larkmidtable.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.larkmidtable.admin.exception.TokenIsExpiredException;
import com.larkmidtable.admin.mapper.JobUserMapper;
import com.larkmidtable.admin.core.util.I18nUtil;
import com.larkmidtable.admin.util.JwtTokenUtils;
import com.larkmidtable.core.biz.model.ReturnT;
import com.larkmidtable.admin.entity.JobUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.larkmidtable.core.biz.model.ReturnT.FAIL_CODE;

@RestController
@RequestMapping("/api/user")
@Api(tags = "基础建设-用户管理模块")
public class BaseUserController {

    @Resource
    private JobUserMapper jobUserMapper;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;


    @GetMapping("/pageList")
    @ApiOperation("用户列表")
    public ReturnT<Map<String, Object>> pageList(@RequestParam(value = "current", required = false, defaultValue = "1") int current,
                                                 @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                                                 @RequestParam(value = "username", required = false) String username) {

        // page list
        List<JobUser> list = jobUserMapper.pageList((current - 1) * size, size, username);
        int recordsTotal = jobUserMapper.pageListCount((current - 1) * size, size, username);

        // package result
        Map<String, Object> maps = new HashMap<>();
        maps.put("recordsTotal", recordsTotal);        // 总记录数
        maps.put("recordsFiltered", recordsTotal);    // 过滤后的总记录数
        maps.put("data", list);                    // 分页列表
        return new ReturnT<>(maps);
    }

    @GetMapping("/list")
    @ApiOperation("用户列表")
    public ReturnT<List<JobUser>> list(String username) {

        // page list
        List<JobUser> list = jobUserMapper.findAll(username);
        return new ReturnT<>(list);
    }

    @GetMapping("/getUserById")
    @ApiOperation(value = "根据id获取用户")
    public ReturnT<JobUser> selectById(@RequestParam("userId") Integer userId) {
        return new ReturnT<>(jobUserMapper.getUserById(userId));
    }

//    @PostMapping("/add")
//    @ApiOperation("添加用户")
//    public ReturnT<String> add(@RequestBody JobUser jobUser) {
//
//        // valid username
//        if (!StringUtils.hasText(jobUser.getUsername())) {
//            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username"));
//        }
//        jobUser.setUsername(jobUser.getUsername().trim());
//        if (!(jobUser.getUsername().length() >= 4 && jobUser.getUsername().length() <= 20)) {
//            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
//        }
//        // valid password
//        if (!StringUtils.hasText(jobUser.getPassword())) {
//            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_password"));
//        }
//        jobUser.setPassword(jobUser.getPassword().trim());
//        if (!(jobUser.getPassword().length() >= 4 && jobUser.getPassword().length() <= 20)) {
//            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
//        }
//        jobUser.setPassword(bCryptPasswordEncoder.encode(jobUser.getPassword()));
//
//
//        // check repeat
//        JobUser existUser = jobUserMapper.loadByUserName(jobUser.getUsername());
//        if (existUser != null) {
//            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("user_username_repeat"));
//        }
//
//        // write
//        jobUserMapper.save(jobUser);
//        return ReturnT.SUCCESS;
//    }

    @PostMapping(value = "/update")
    @ApiOperation("更新用户信息")
    public ReturnT<String> update(HttpServletRequest request,@RequestBody JobUser jobUser)
			throws TokenIsExpiredException {
		String tokenHeader = request.getHeader(JwtTokenUtils.TOKEN_HEADER);
		String userName = getUserName(tokenHeader);
		if(jobUser.getUsername().equals(userName)) {
			if (StringUtils.hasText(jobUser.getPassword())) {
				String pwd = jobUser.getPassword().trim();
				if (StrUtil.isBlank(pwd)) {
					return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_no_blank") + "密码");
				}

				if (!(pwd.length() >= 4 && pwd.length() <= 20)) {
					return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
				}
				jobUser.setPassword(bCryptPasswordEncoder.encode(pwd));
			} else {
				return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_no_blank") + "密码");
			}
			// write
			jobUserMapper.update(jobUser);
			return ReturnT.SUCCESS;
		}else {
			return new ReturnT<>(FAIL_CODE,   "不能修改其他用户名的密码");
		}
    }

	private String getUserName(String tokenHeader) throws TokenIsExpiredException {
		String token = tokenHeader.replace(JwtTokenUtils.TOKEN_PREFIX, "");
		boolean expiration = JwtTokenUtils.isExpiration(token);
		if (expiration) {
			throw new TokenIsExpiredException("登录时间过长，请退出重新登录");
		}
		else {
			String username = JwtTokenUtils.getUsername(token);
			return username;
		}
	}


//    @RequestMapping(value = "/remove", method = RequestMethod.POST)
//    @ApiOperation("删除用户")
//    public ReturnT<String> remove(int id) {
//        int result = jobUserMapper.delete(id);
//        return result != 1 ? ReturnT.FAIL : ReturnT.SUCCESS;
//    }

    @PostMapping(value = "/updatePwd")
    @ApiOperation("修改密码")
    public ReturnT<String> updatePwd(@RequestBody JobUser jobUser) {
        String password = jobUser.getPassword();
        if (password == null || password.trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        password = password.trim();
        if (!(password.length() >= 4 && password.length() <= 20)) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
        }
        // do write
        JobUser existUser = jobUserMapper.loadByUserName(jobUser.getUsername());
        existUser.setPassword(bCryptPasswordEncoder.encode(password));
        jobUserMapper.update(existUser);
        return ReturnT.SUCCESS;
    }

}
