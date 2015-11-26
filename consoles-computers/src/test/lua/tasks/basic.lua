
return function()
    log("testIntegralValue: " .. testIntegralValue);
    if (testIntegralValue ~= 42) then
        error("failed test: failed to pass expected value")
    end
end