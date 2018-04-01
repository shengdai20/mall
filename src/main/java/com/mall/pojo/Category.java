package com.mall.pojo;

import lombok.*;

import java.util.Date;

//@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
//只针对id属性生成equals和hashCode方法
@EqualsAndHashCode(of = "id")
//多个字段比对，用数组实现
//@EqualsAndHashCode(of = {"id", "name"})
//不拼接updateTime属性
//@ToString(exclude = "updateTime")
public class Category {
    private Integer id;

    private Integer parentId;

    private String name;

    private Boolean status;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    //重写equals和hashCode方法来进行排重，并且保证两者的判断因子是一样的，例如这里都利用id判断
    //equals和hashCode关系：如果两个对象相同，即用equlas比较返回true，则他们的hashCode值一定要相同
    //如果两个对象的hashCode相同，他们并不一定相同，即hashCode相同，用equlas比较也可能返回false，为什么？
    //因为hashCode只是取了id的hashCode作为一个因子，而我们的equals中可以把其他属性放入综合判定是否相同
    /*@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Category category = (Category) o;

        return !(id != null ? !id.equals(category.id) : category.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }*/
}