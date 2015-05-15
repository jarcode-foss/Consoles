-- Written by Jarcode

List {
    arr = {},

    add = add,
    append = append,
    remove = remove,
    size = size,
    table = table
}
List.__index = List;

function List.new()
    local self = setmetatable({}, List);
    return self;
end
function List:add(index, elem)
    if index < 1 then return end
    if index > self:size() then self:append(elem) end
    local n_arr = {}
    for i = 1, index - 1 do
        n_arr[i] = self.arr[i];
    end
    n_arr[index] = elem;
    for i = index, self:size() do
        n_arr[i + 1] = self.arr[i]
    end
    self.arr = n_arr;
end
function List:remove(index)
    if index > self:size() or index < 1 then return end
    local n_arr = {}
    for i = 1, index - 1 do
        n_arr[i] = self.arr[i];
    end
    for i = index + 1, self:size() do
       n_arr[i] = self.arr[i]
    end
    self.arr = n_arr;
end
function List:append(elem)
    self.arr[self:size() + 1] = elem;
end
function List:size()
    return #self.arr
end
function List:table()
    local ret = {}
    for i = 1, self:size() do
        ret[i] = self.arr[i];
    end
    return ret;
end
function split(input, separator)
    if separator == nil then return end
    local t = {}
    local i = 1
    for str in string.gmatch(input, "([^" .. sep .. "]+)") do
        t[i] = str
        i = i + 1
    end
    return t
end

