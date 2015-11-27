
return function()
    local status, err = pcall(throwSomething())
    if (status == true) then
        error("no error recieved");
    else
        log("recieved error: " .. err)
    end
end