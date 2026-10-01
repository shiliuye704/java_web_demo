package com.sky.service;

import com.sky.entity.AddressBook;
import com.sky.result.Result;

import java.util.List;

public interface AddressBookService {
    void add(AddressBook addressBook);


    List<AddressBook> list();

    AddressBook getById(Integer id);

    void delete(Integer id);

    void setDefault(AddressBook addressBook);

    AddressBook getDefault();

}
