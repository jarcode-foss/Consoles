
return function()
    -- get rid of internal debug messages, as any java-based exception will
    -- be printed during debug mode.
    setdebug(false)
    local status, err = pcall(function() throwSomething() end)
    if (status == true) then
        error("no error recieved");
    else
        log("recieved error: " .. err)
    end
    -- restore debug messages
    setdebug(true)
end