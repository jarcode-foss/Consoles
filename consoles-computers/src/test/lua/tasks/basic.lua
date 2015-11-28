
return function()
    log("__impl: " .. tostring(__impl))
    log("__JNIENV: " .. tostring(__JNIENV))
    log("testIntegralValue: " .. testIntegralValue)
    if (testIntegralValue ~= 42) then
        error("failed test: failed to pass expected value")
    end
end