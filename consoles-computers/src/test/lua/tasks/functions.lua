
return function()
    log("__function registry:")
    for i, v in ipairs(__functions) do
        log("    " .. tostring(i) .. " -> " .. tostring(v))
    end
    log(lambdaTestFunction(42, "foo"))
    log(testFunction(42, "foo"))
end