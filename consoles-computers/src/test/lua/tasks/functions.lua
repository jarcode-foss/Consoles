
return function()

    -- tests a function for a return value
    local assert = function(func, val)
        local ret = _G[func]();
        if ret ~= val then
            error("assertion error: " .. func .. ", got: " .. tostring(ret))
        end
    end

    log("__function registry:")
    for i, v in ipairs(__functions) do
        log("    " .. tostring(i) .. " -> " .. tostring(v))
    end
    log(lambdaTestFunction(42, "foo"))
    log(testFunction(42, "foo"))

    assert("testIntReturn", 42)
    assert("testStringReturn", "foobar")
end