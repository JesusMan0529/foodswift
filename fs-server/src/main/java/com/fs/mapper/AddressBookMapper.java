package com.fs.mapper;

import com.fs.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    /**
     * 默认地址排在最前面，推荐功能优先使用默认地址。
     */
    @Select("select * from address_book where user_id = #{userId} " +
            "order by is_default desc, id desc")
    List<AddressBook> listByUserId(Long userId);
}
