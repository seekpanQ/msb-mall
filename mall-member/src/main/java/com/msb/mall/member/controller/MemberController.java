package com.msb.mall.member.controller;

import com.msb.common.exception.BizCodeEnum;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.R;
import com.msb.mall.member.entity.MemberEntity;
import com.msb.mall.member.exception.PhoneExsitExecption;
import com.msb.mall.member.exception.UsernameExsitException;
import com.msb.mall.member.service.MemberService;
import com.msb.mall.member.vo.MemberLoginVO;
import com.msb.mall.member.vo.MemberReigerVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * 会员
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 16:38:34
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    /**
     * 会员注册
     *
     * @param vo
     * @return
     */
    @PostMapping("/register")
    public R register(@RequestBody MemberReigerVO vo) {
        try {
            memberService.register(vo);
        } catch (UsernameExsitException exception) {
            return R.error(BizCodeEnum.USERNAME_EXSIT_EXCEPTION.getCode(),
                    BizCodeEnum.USERNAME_EXSIT_EXCEPTION.getMsg());
        } catch (PhoneExsitExecption execption) {
            return R.error(BizCodeEnum.PHONE_EXSIT_EXCEPTION.getCode(),
                    BizCodeEnum.PHONE_EXSIT_EXCEPTION.getMsg());
        } catch (Exception exception) {
            exception.printStackTrace();
            return R.error(BizCodeEnum.UNKNOW_EXCEPTION.getCode(),
                    BizCodeEnum.UNKNOW_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    @RequestMapping("/login")
    public R login(@RequestBody MemberLoginVO vo) {
        MemberEntity entity = memberService.login(vo);
        if (entity != null) {
            return R.ok();
        }
        return R.error(BizCodeEnum.USERNAME_PHONE_VALID_EXCEPTION.getCode(),
                BizCodeEnum.USERNAME_PHONE_VALID_EXCEPTION.getMsg());
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
