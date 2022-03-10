package com.lif314.gulimall.member.service.impl;

import com.lif314.gulimall.member.entity.MemberLevelEntity;
import com.lif314.gulimall.member.exception.PhoneExistException;
import com.lif314.gulimall.member.exception.UsernameExistException;
import com.lif314.gulimall.member.vo.MemberRegisterVo;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.member.dao.MemberDao;
import com.lif314.gulimall.member.entity.MemberEntity;
import com.lif314.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册用户信息
     */
    @Override
    public void register(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();

        // 先要判断用户名和手机号是否已经存在
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        // 获取默认等级id的ID
        MemberLevelEntity memberLevel = getDefaultMemberLevel();
        memberEntity.setLevelId(memberLevel.getId());

        memberEntity.setUsername(vo.getUserName());
        memberEntity.setMobile(vo.getPhone());

        // 密码加密存储

        memberEntity.setPassword(vo.getPassword());


        // 保存数据
        this.baseMapper.insert(memberEntity);

    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameExistException {
        Long userCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(userCount > 0L){
            throw new UsernameExistException();
        }
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Long mobile = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(mobile > 0L){
            throw new PhoneExistException();
        }
    }


    private MemberLevelEntity getDefaultMemberLevel() {
        return this.baseMapper.getDefaultMemberLevel();
    }

}
